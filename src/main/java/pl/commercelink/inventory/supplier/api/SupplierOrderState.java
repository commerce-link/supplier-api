package pl.commercelink.inventory.supplier.api;

/**
 * Coarse lifecycle of an order at the supplier as seen by {@link SupplierProvider#trackOrder}.
 * {@code PARTIALLY_SHIPPED}/{@code SHIPPED} carry the parcels shipped so far; {@code CANCELLED}
 * means the supplier will not ship (the application never reverts anything on its own).
 */
public enum SupplierOrderState {
    PROCESSING,
    PARTIALLY_SHIPPED,
    SHIPPED,
    CANCELLED
}
