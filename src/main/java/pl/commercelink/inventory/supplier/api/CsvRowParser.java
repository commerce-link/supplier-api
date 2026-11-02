package pl.commercelink.inventory.supplier.api;

public interface CsvRowParser {

    FeedItem parse(String[] row);
}
