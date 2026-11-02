package pl.commercelink.inventory.supplier.api;

public interface FeedItem {
    String getEan();
    String getMfn();
    String getBrand();
    String getName();
    String getCategory();
    double getNetPrice();
    String getCurrency();
    int getQty();

    default int getLeadTimeDays() { return 0; }
    default boolean isSellable() { return true; }
    default boolean isInStock() { return false; }
    default boolean isInDelivery() { return false; }
    default String getSku() { return null; }
    default Integer getNetWeightInGrams() { return null; }
    default Integer getGrossWeightInGrams() { return null; }
}
