package pl.commercelink.inventory.supplier.api;

public record SupplierQuote(String ean, String mfn, int availableQuantity,
                            double netPrice, String currency) {}
