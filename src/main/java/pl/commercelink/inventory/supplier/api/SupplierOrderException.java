package pl.commercelink.inventory.supplier.api;

public class SupplierOrderException extends RuntimeException {

    public SupplierOrderException(String message) {
        super(message);
    }

    public SupplierOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
