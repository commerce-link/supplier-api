package pl.commercelink.inventory.supplier.api;

import java.util.List;

public record SupplierPurchaseRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                      String deliveryAddressId) {

    public SupplierPurchaseRequest(String clientOrderRef, List<SupplierOrderLine> lines) {
        this(clientOrderRef, lines, null);
    }
}
