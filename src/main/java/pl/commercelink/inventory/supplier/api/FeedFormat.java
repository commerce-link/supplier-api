package pl.commercelink.inventory.supplier.api;

public sealed interface FeedFormat {

    record Csv(CsvRowParser parser, char separator) implements FeedFormat {}

    record Xml(Class<? extends XmlWrapper> wrapperClass) implements FeedFormat {}

}
