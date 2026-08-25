package pl.commercelink.inventory.supplier.api.testing;

import pl.commercelink.inventory.supplier.api.FeedData;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderLookup;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierOrderState;
import pl.commercelink.inventory.supplier.api.SupplierOrderTracking;
import pl.commercelink.inventory.supplier.api.SupplierParcel;
import pl.commercelink.inventory.supplier.api.SupplierProvider;
import pl.commercelink.inventory.supplier.api.SupplierPurchaseRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

class InMemoryTrackingProviderContractTest extends SupplierOrderTrackingContractTest {

    static class InMemoryProvider implements SupplierProvider {
        final Map<String, String> ordersByRef = new HashMap<>();
        final Set<String> shipped = new HashSet<>();
        final boolean failing;

        InMemoryProvider(boolean failing) {
            this.failing = failing;
        }

        @Override
        public Optional<FeedData> download() {
            return Optional.empty();
        }

        @Override
        public boolean supportsOrdering() {
            return true;
        }

        @Override
        public SupplierOrderResult placeOrder(SupplierPurchaseRequest request) {
            String id = ordersByRef.computeIfAbsent(request.clientOrderRef(), ref -> "PO-" + ref);
            return new SupplierOrderResult(id, 1.0, "PLN", List.of());
        }

        @Override
        public boolean supportsOrderTracking() {
            return true;
        }

        @Override
        public Optional<SupplierOrderTracking> trackOrder(SupplierOrderLookup lookup) {
            if (failing) {
                throw new SupplierOrderException("boom");
            }
            String id = lookup.externalOrderId() != null && ordersByRef.containsValue(lookup.externalOrderId())
                    ? lookup.externalOrderId()
                    : ordersByRef.get(lookup.clientOrderRef());
            if (id == null) {
                return Optional.empty();
            }
            if (shipped.contains(id)) {
                return Optional.of(new SupplierOrderTracking(SupplierOrderState.SHIPPED,
                        List.of(new SupplierParcel("DPD", "TRK-" + id, null, null, null))));
            }
            return Optional.of(new SupplierOrderTracking(SupplierOrderState.PROCESSING, List.of()));
        }
    }

    private final InMemoryProvider provider = new InMemoryProvider(false);

    @Override
    protected SupplierProvider trackingProvider() {
        return provider;
    }

    @Override
    protected List<SupplierOrderLine> sampleLines() {
        return List.of(new SupplierOrderLine("sku-1", "5900000000001", "MFN-1", 1));
    }

    @Override
    protected String uniqueClientOrderRef() {
        return UUID.randomUUID().toString();
    }

    @Override
    protected SupplierOrderResult placeSampleOrder(SupplierProvider p, String clientOrderRef) {
        return p.placeOrder(new SupplierPurchaseRequest(clientOrderRef, sampleLines(), null));
    }

    @Override
    protected boolean advanceToShipped(SupplierProvider p, String clientOrderRef, String externalOrderId) {
        provider.shipped.add(externalOrderId);
        return true;
    }

    @Override
    protected Optional<SupplierProvider> trackingProviderWithFailingBackend() {
        return Optional.of(new InMemoryProvider(true));
    }

    @Override
    protected OptionalInt remotePlacedOrders() {
        return OptionalInt.of(provider.ordersByRef.size());
    }
}
