package pl.commercelink.inventory.supplier.api;

import java.util.List;
import java.util.Map;

/**
 * Dropship order: lines shipped by the supplier straight to {@code consignee}. When
 * {@code pickupPoint} is present the parcel goes to that carrier point instead of the consignee's
 * street address; the consignee still carries the recipient's name and contact data. {@code options}
 * carries the operator's answers to {@link SupplierProvider#orderOptions(SupplierOrderOptionsContext)}
 * keyed by option key; never null.
 */
public record SupplierDropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                      SupplierConsignee consignee, String deliveryInstructions,
                                      SupplierPickupPoint pickupPoint, Map<String, String> options) {
    public SupplierDropshipRequest {
        options = options == null ? Map.of() : Map.copyOf(options);
    }

    public SupplierDropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                   SupplierConsignee consignee) {
        this(clientOrderRef, lines, consignee, null, null, Map.of());
    }

    public SupplierDropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                   SupplierConsignee consignee, String deliveryInstructions) {
        this(clientOrderRef, lines, consignee, deliveryInstructions, null, Map.of());
    }

    public SupplierDropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                   SupplierConsignee consignee, String deliveryInstructions,
                                   SupplierPickupPoint pickupPoint) {
        this(clientOrderRef, lines, consignee, deliveryInstructions, pickupPoint, Map.of());
    }
}
