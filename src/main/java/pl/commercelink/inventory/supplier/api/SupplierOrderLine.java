package pl.commercelink.inventory.supplier.api;

public record SupplierOrderLine(String ean, String mfn, int quantity) {}
