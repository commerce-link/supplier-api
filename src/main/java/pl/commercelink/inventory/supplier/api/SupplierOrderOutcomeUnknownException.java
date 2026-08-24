package pl.commercelink.inventory.supplier.api;

/**
 * The purchase request may have reached the supplier but the outcome could not
 * be confirmed (timeout, 5xx, connection reset, failure after the request left
 * the process). The order MAY exist — a blind retry risks a duplicate.
 */
public class SupplierOrderOutcomeUnknownException extends SupplierOrderException {

    public SupplierOrderOutcomeUnknownException(String message) {
        super(message);
    }

    public SupplierOrderOutcomeUnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
