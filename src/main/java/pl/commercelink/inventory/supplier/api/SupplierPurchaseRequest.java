package pl.commercelink.inventory.supplier.api;

import java.util.List;

public record SupplierPurchaseRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                      String deliveryAddressId, SupplierShippingAddress shippingAddress) {

    public SupplierPurchaseRequest(String clientOrderRef, List<SupplierOrderLine> lines) {
        this(clientOrderRef, lines, null, null);
    }

    public SupplierPurchaseRequest(String clientOrderRef, List<SupplierOrderLine> lines,
                                   String deliveryAddressId) {
        this(clientOrderRef, lines, deliveryAddressId, null);
    }
}
