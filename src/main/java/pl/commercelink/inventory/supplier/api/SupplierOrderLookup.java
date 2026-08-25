package pl.commercelink.inventory.supplier.api;

/**
 * Identifiers of an order previously placed at the supplier, used to look it up read-only.
 * {@code externalOrderId} is the supplier's own number, {@code clientOrderRef} ours; at least
 * one must be present. Adapters use whichever their API indexes by.
 */
public record SupplierOrderLookup(String externalOrderId, String clientOrderRef) {

    public SupplierOrderLookup {
        externalOrderId = trimToNull(externalOrderId);
        clientOrderRef = trimToNull(clientOrderRef);
        if (externalOrderId == null && clientOrderRef == null) {
            throw new IllegalArgumentException("Order lookup needs an external order id or a client order ref");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
