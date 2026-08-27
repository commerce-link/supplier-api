package pl.commercelink.inventory.supplier.api.testing;

import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.FeedData;
import pl.commercelink.inventory.supplier.api.SupplierDropshipRequest;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderOption;
import pl.commercelink.inventory.supplier.api.SupplierOrderOptionChoice;
import pl.commercelink.inventory.supplier.api.SupplierOrderOptionsContext;
import pl.commercelink.inventory.supplier.api.SupplierOrderOutcomeUnknownException;
import pl.commercelink.inventory.supplier.api.SupplierOrderRejectedException;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierPickupPoint;
import pl.commercelink.inventory.supplier.api.SupplierProvider;
import pl.commercelink.inventory.supplier.api.SupplierPurchaseRequest;
import pl.commercelink.inventory.supplier.api.ordering.IdempotentOrderPlacement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryDropshipProviderContractTest extends SupplierDropshipContractTest {

    enum Mode { OK, SHORTAGE, FAILING, TRANSPORT_FAILURE, REJECTING }

    static class InMemoryProvider extends IdempotentOrderPlacement<String, String> implements SupplierProvider {
        final Map<String, String> ordersByRef = new HashMap<>();
        final Mode mode;
        final boolean pickupPoints;
        int remoteCalls;
        String lastPickupCode;

        InMemoryProvider(Mode mode, boolean pickupPoints) {
            this.mode = mode;
            this.pickupPoints = pickupPoints;
        }

        @Override public Optional<FeedData> download() { return Optional.empty(); }
        @Override public boolean supportsDropshipping() { return true; }
        @Override public boolean supportsPickupPointDropship() { return pickupPoints; }
        @Override public List<SupplierOrderOption> orderOptions(SupplierOrderOptionsContext context) {
            // Real adapters fetch this from the supplier, so count it as a remote call here too —
            // that is what proves the ref/consignee/pickup guard tests never consult it. Per the
            // interface contract, a failure here must surface only as SupplierOrderException.
            try {
                remote();
            } catch (RuntimeException e) {
                throw new SupplierOrderException("InMemory order options failed", e);
            }
            return List.of(new SupplierOrderOption("lane", "Lane", List.of(
                    new SupplierOrderOptionChoice("fast", "Fast", null),
                    new SupplierOrderOptionChoice("slow", "Slow", null)), "fast", true));
        }
        @Override public SupplierOrderResult placeDropshipOrder(SupplierDropshipRequest request) {
            return placeDropshipIdempotently(request);
        }
        @Override protected String supplierName() { return "InMemory"; }
        @Override protected String toSupplierLine(SupplierOrderLine line) { return line.sku(); }
        @Override protected Optional<String> findExistingOrder(String clientOrderRef) {
            remote();
            return Optional.ofNullable(ordersByRef.get(clientOrderRef));
        }
        @Override protected String placeNewOrder(SupplierPurchaseRequest request, List<String> lines) {
            validateLane(request.options());
            throw new UnsupportedOperationException();
        }
        @Override protected String placeNewDropshipOrder(SupplierDropshipRequest request, List<String> lines) {
            validateLane(request.options());
            remote();
            lastPickupCode = request.pickupPoint() == null ? null : request.pickupPoint().code();
            switch (mode) {
                case SHORTAGE, REJECTING -> throw new SupplierOrderRejectedException("rejected");
                case TRANSPORT_FAILURE -> throw new IllegalStateException("connection reset");
                default -> { }
            }
            return ordersByRef.computeIfAbsent(request.clientOrderRef(), ref -> "DS-" + ref);
        }
        @Override protected String externalOrderId(String order) { return order; }
        @Override protected SupplierOrderResult toResult(String order, SupplierPurchaseRequest request) {
            return new SupplierOrderResult(order, 1.0, "PLN", List.of());
        }
        private void remote() {
            remoteCalls++;
            if (mode == Mode.FAILING) throw new IllegalStateException("backend down");
        }
        private void validateLane(Map<String, String> options) {
            String lane = options.get("lane");
            if (!"fast".equals(lane) && !"slow".equals(lane)) {
                throw new SupplierOrderRejectedException("Unknown lane");
            }
        }
    }

    private InMemoryProvider last;
    private int hookInvocations;

    /**
     * Reuses the current provider when asked for the same (mode, pickupPoints) combination it
     * already holds, so a helper that looks up sample options via a hook (e.g. {@code
     * dropshipRequest}) does not silently swap out the provider instance a test is placing an
     * order against. This memoization is now belt-and-braces: the kit no longer re-invokes the
     * provider hook to build a request (see {@link #placingAnOrderInvokesTheProviderHookExactlyOnce}),
     * but it is kept because it is harmless and guards against a future regression of the same kind.
     */
    private InMemoryProvider provider(Mode mode, boolean pickupPoints) {
        hookInvocations++;
        if (last != null && last.mode == mode && last.pickupPoints == pickupPoints) {
            return last;
        }
        last = new InMemoryProvider(mode, pickupPoints);
        return last;
    }

    /**
     * Guards against the request-builder helpers ({@code dropshipRequest}) re-invoking the
     * provider hook to compute sample options: adapters whose hook builds a fresh fake per call
     * (e.g. Elko) would then fail idempotency tests even though no test code calls the hook twice.
     */
    @Test
    void placingAnOrderInvokesTheProviderHookExactlyOnce() {
        // given
        hookInvocations = 0;

        // when
        SupplierProvider provider = dropshipProvider();
        place(provider, uniqueClientOrderRef(), sampleLines());

        // then
        assertEquals(1, hookInvocations);
    }

    @Override protected SupplierProvider dropshipProvider() { return provider(Mode.OK, true); }
    @Override protected SupplierProvider dropshipProviderWithShortage() { return provider(Mode.SHORTAGE, true); }
    @Override protected List<SupplierOrderLine> sampleLines() {
        return List.of(new SupplierOrderLine("sku-1", "5900000000001", "MFN-1", 1));
    }
    @Override protected String uniqueClientOrderRef() { return UUID.randomUUID().toString(); }
    @Override protected Optional<SupplierProvider> dropshipProviderWithFailingBackend() {
        return Optional.of(provider(Mode.FAILING, true));
    }
    @Override protected Optional<SupplierProvider> dropshipProviderWithPlacementTransportFailure() {
        return Optional.of(provider(Mode.TRANSPORT_FAILURE, true));
    }
    @Override protected Optional<SupplierProvider> dropshipProviderRejectingOrders() {
        return Optional.of(provider(Mode.REJECTING, true));
    }
    @Override protected Optional<SupplierPickupPoint> samplePickupPoint() {
        return Optional.of(new SupplierPickupPoint("InPost", "WAW04A", null, null, null, null));
    }
    @Override protected Optional<SupplierProvider> dropshipProviderWithoutPickupPoints() {
        return Optional.of(provider(Mode.OK, false));
    }
    @Override protected OptionalInt remoteDropshipOrdersPlaced() { return OptionalInt.of(last.ordersByRef.size()); }
    @Override protected OptionalInt remoteCalls() { return OptionalInt.of(last.remoteCalls); }
    @Override protected Optional<String> remotePickupPointCode() { return Optional.ofNullable(last.lastPickupCode); }
}
