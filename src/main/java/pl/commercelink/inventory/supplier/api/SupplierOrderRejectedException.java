package pl.commercelink.inventory.supplier.api;

/**
 * The supplier definitively did not create the order: an explicit rejection,
 * a pre-send validation failure, or an availability shortage. Retrying the
 * purchase cannot produce a duplicate.
 */
public class SupplierOrderRejectedException extends SupplierOrderException {

    public SupplierOrderRejectedException(String message) {
        super(message);
    }

    public SupplierOrderRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
