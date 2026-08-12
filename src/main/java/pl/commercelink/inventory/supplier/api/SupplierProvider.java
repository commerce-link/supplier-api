package pl.commercelink.inventory.supplier.api;

import pl.commercelink.inventory.supplier.api.support.ResourceDownloadException;

import java.util.List;
import java.util.Optional;

public interface SupplierProvider {

    Optional<FeedData> download() throws ResourceDownloadException;

    default boolean supportsOrdering() {
        return false;
    }

    /**
     * Whether the supplier refuses an order that does not name one of the delivery addresses
     * registered on the account. Providers answering {@code true} implement
     * {@link #deliveryAddresses()}; the application blocks ordering until the address is chosen.
     */
    default boolean requiresDeliveryAddress() {
        return false;
    }

    /**
     * Delivery addresses registered on the store's account at the supplier, fetched remotely.
     * Providers that require an address fail with {@link SupplierOrderException} when the list
     * cannot be retrieved or comes back empty — an order shipped to a guessed address is worse
     * than no order at all.
     */
    default List<SupplierDeliveryAddress> deliveryAddresses() {
        return List.of();
    }

    default List<SupplierQuote> checkAvailability(List<SupplierOrderLine> lines) {
        throw new UnsupportedOperationException("Ordering not supported by this supplier");
    }

    default SupplierOrderResult placeOrder(SupplierPurchaseRequest request) {
        throw new UnsupportedOperationException("Ordering not supported by this supplier");
    }
}
