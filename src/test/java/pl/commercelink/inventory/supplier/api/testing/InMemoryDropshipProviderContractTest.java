package pl.commercelink.inventory.supplier.api.testing;

import pl.commercelink.inventory.supplier.api.FeedData;
import pl.commercelink.inventory.supplier.api.SupplierDropshipRequest;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
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

class InMemoryDropshipProviderContractTest extends SupplierDropshipContractTest {

    enum Mode { OK, SHORTAGE, FAILING, TRANSPORT_FAILURE, REJECTING }

    static class InMemoryProvider extends IdempotentOrderPlacement<String, String> implements SupplierProvider {
        final Map<String, String> ordersByRef = new HashMap<>();
        final Mode mode;
        final boolean pickupPoints;
        int remoteCalls;

        InMemoryProvider(Mode mode, boolean pickupPoints) {
            this.mode = mode;
            this.pickupPoints = pickupPoints;
        }

        @Override public Optional<FeedData> download() { return Optional.empty(); }
        @Override public boolean supportsDropshipping() { return true; }
        @Override public boolean supportsPickupPointDropship() { return pickupPoints; }
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
            throw new UnsupportedOperationException();
        }
        @Override protected String placeNewDropshipOrder(SupplierDropshipRequest request, List<String> lines) {
            remote();
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
    }

    private InMemoryProvider last;

    private InMemoryProvider provider(Mode mode, boolean pickupPoints) {
        last = new InMemoryProvider(mode, pickupPoints);
        return last;
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
}
