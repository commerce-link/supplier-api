package pl.commercelink.inventory.supplier.api;

import pl.commercelink.inventory.supplier.api.support.ResourceDownloadException;

import java.util.List;
import java.util.Optional;

public interface SupplierProvider {

    Optional<FeedData> download() throws ResourceDownloadException;

    default boolean supportsOrdering() {
        return false;
    }

    default List<SupplierQuote> checkAvailability(List<SupplierOrderLine> lines) {
        throw new UnsupportedOperationException("Ordering not supported by this supplier");
    }

    default SupplierOrderResult placeOrder(SupplierPurchaseRequest request) {
        throw new UnsupportedOperationException("Ordering not supported by this supplier");
    }
}
