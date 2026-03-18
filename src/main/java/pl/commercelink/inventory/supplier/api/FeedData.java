package pl.commercelink.inventory.supplier.api;

public record FeedData(byte[] data, String extension) {

    public static FeedData csv(byte[] data) {
        return new FeedData(data, "csv");
    }

    public static FeedData xml(byte[] data) {
        return new FeedData(data, "xml");
    }

}
