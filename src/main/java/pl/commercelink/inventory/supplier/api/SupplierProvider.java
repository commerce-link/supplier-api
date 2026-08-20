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

    /**
     * Whether the supplier can ship an order directly to the end customer (dropshipping).
     * Independent from {@link #supportsOrdering()}: real suppliers expose dropshipping as a
     * different endpoint with its own contract (e.g. ELKO {@code /Orders/EndUser}) and it may
     * hinge on a separate business agreement, so an adapter can support either capability
     * without the other.
     */
    default boolean supportsDropshipping() {
        return false;
    }

    /**
     * Places an order shipped by the supplier straight to {@link SupplierDropshipRequest#consignee()}.
     * Same rules as {@link #placeOrder}: idempotent per {@code clientOrderRef}, all-or-nothing on
     * availability, failures surface only as {@link SupplierOrderException}.
     */
    default SupplierOrderResult placeDropshipOrder(SupplierDropshipRequest request) {
        throw new UnsupportedOperationException("Dropshipping not supported by this supplier");
    }

    default List<SupplierQuote> checkAvailability(List<SupplierOrderLine> lines) {
        throw new UnsupportedOperationException("Ordering not supported by this supplier");
    }

    default SupplierOrderResult placeOrder(SupplierPurchaseRequest request) {
        throw new UnsupportedOperationException("Ordering not supported by this supplier");
    }

    /**
     * Final order number for an order previously placed with the given client order ref, once
     * the supplier considers it fully registered. Empty while the supplier is still processing
     * the order (or when the supplier never reports a later number). Communication failures
     * surface as {@link SupplierOrderException}.
     */
    default Optional<String> confirmedOrderId(String clientOrderRef) {
        return Optional.empty();
    }
}
