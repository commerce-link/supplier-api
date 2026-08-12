package pl.commercelink.inventory.supplier.api.support;

@FunctionalInterface
public interface FeedSource {

    byte[] fetch() throws ResourceDownloadException;
}
