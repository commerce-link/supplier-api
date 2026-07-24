package pl.commercelink.inventory.supplier.api;

public interface XmlItem {
    String getEan();
    String getMfn();
    String getBrand();
    String getName();
    String getCategory();
    double getNetPrice();
    int getQty();
    String getCurrency();

    default boolean isSellable() { return true; }
    default boolean isInStock() { return false; }
    default boolean isInDelivery() { return false; }
    default Integer getNetWeightInGrams() { return null; }
    default Integer getGrossWeightInGrams() { return null; }
    default String getRawCategory() { return null; }

    default ParsedRow toParsedRow(SupplierInfo supplierInfo) {
        var item = new InventoryItem(getEan(), getMfn(), getNetPrice(), getCurrency(),
                getQty(), 0, supplierInfo.name(),
                isSellable(), isInStock(), isInDelivery());
        var product = new SupplierProduct(getEan(), getMfn(), getBrand(), getName(),
                supplierInfo.accuracyScore(),
                getNetWeightInGrams(), getGrossWeightInGrams(),
                getRawCategory() != null ? getRawCategory() : getCategory());
        return new ParsedRow(item, product);
    }
}
