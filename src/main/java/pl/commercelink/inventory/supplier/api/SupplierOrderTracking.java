package pl.commercelink.inventory.supplier.api;

import java.util.List;

/** Snapshot of an order at the supplier: its state and the parcels shipped so far. */
public record SupplierOrderTracking(SupplierOrderState state, List<SupplierParcel> parcels) {

    public SupplierOrderTracking {
        if (state == null) {
            throw new IllegalArgumentException("Order tracking state is required");
        }
        parcels = parcels == null ? List.of() : List.copyOf(parcels);
    }
}
