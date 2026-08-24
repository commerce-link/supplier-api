package pl.commercelink.inventory.supplier.api;

import java.util.List;

/**
 * A dropship order: goods go straight from the supplier to the {@link SupplierConsignee}. The
 * {@code clientOrderRef} carries the same idempotency contract as regular purchases; providers
 * may forward {@code deliveryInstructions} to the supplier when the API has such a field.
 */
public record SupplierDropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                      SupplierConsignee consignee, String deliveryInstructions) {

    public SupplierDropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                   SupplierConsignee consignee) {
        this(clientOrderRef, lines, consignee, null);
    }
}
