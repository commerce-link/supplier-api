package pl.commercelink.inventory.supplier.api;

import pl.commercelink.taxonomy.ProductCategory;

public interface XmlItem {
    String getEan();
    String getMfn();
    String getBrand();
    String getName();
    ProductCategory getCategory();
    double getNetPrice();
    int getQty();
    String getCurrency();

    default boolean isSellable() { return true; }
    default boolean isInStock() { return false; }
    default boolean isInDelivery() { return false; }
    default Integer getNetWeightInGrams() { return null; }
    default Integer getGrossWeightInGrams() { return null; }

    default ParsedRow toParsedRow(SupplierInfo supplierInfo) {
        var item = new InventoryItem(getEan(), getMfn(), getNetPrice(), getCurrency(),
                getQty(), 0, supplierInfo.name(),
                isSellable(), isInStock(), isInDelivery());
        ProductCategory category = getCategory();
        var taxonomy = new Taxonomy(getEan(), getMfn(), getBrand(), getName(),
                category == null ? null : category.name(), supplierInfo.accuracyScore(),
                getNetWeightInGrams(), getGrossWeightInGrams());
        return new ParsedRow(item, taxonomy);
    }
}
