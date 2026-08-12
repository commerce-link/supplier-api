package pl.commercelink.inventory.supplier.api;

public record SupplierOrderLine(String sku, String ean, String mfn, int quantity) {}
