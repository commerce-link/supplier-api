package pl.commercelink.inventory.supplier.api;

/** What the application knows about an order before asking the supplier which options it needs. */
public record SupplierOrderOptionsContext(boolean dropship, SupplierPickupPoint pickupPoint) {

    public static SupplierOrderOptionsContext warehouse() {
        return new SupplierOrderOptionsContext(false, null);
    }

    public static SupplierOrderOptionsContext dropship(SupplierPickupPoint pickupPoint) {
        return new SupplierOrderOptionsContext(true, pickupPoint);
    }
}
