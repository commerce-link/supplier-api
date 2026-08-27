package pl.commercelink.inventory.supplier.api;

import java.util.List;
import java.util.Map;

/**
 * Warehouse purchase. {@code options} carries the operator's answers to
 * {@link SupplierProvider#orderOptions(SupplierOrderOptionsContext)} keyed by option key; never null.
 */
public record SupplierPurchaseRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                      String deliveryAddressId, Map<String, String> options) {
    public SupplierPurchaseRequest {
        options = options == null ? Map.of() : Map.copyOf(options);
    }

    public SupplierPurchaseRequest(String clientOrderRef, List<SupplierOrderLine> lines) {
        this(clientOrderRef, lines, null, Map.of());
    }

    public SupplierPurchaseRequest(String clientOrderRef, List<SupplierOrderLine> lines, String deliveryAddressId) {
        this(clientOrderRef, lines, deliveryAddressId, Map.of());
    }
}
