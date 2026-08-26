package pl.commercelink.inventory.supplier.api;

import java.util.List;

/**
 * Dropship order: lines shipped by the supplier straight to {@code consignee}. When
 * {@code pickupPoint} is present the parcel goes to that carrier point instead of the consignee's
 * street address; the consignee still carries the recipient's name and contact data.
 */
public record SupplierDropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                      SupplierConsignee consignee, String deliveryInstructions,
                                      SupplierPickupPoint pickupPoint) {

    public SupplierDropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                   SupplierConsignee consignee) {
        this(clientOrderRef, lines, consignee, null, null);
    }

    public SupplierDropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                   SupplierConsignee consignee, String deliveryInstructions) {
        this(clientOrderRef, lines, consignee, deliveryInstructions, null);
    }
}
