package pl.commercelink.inventory.supplier.api;

import pl.commercelink.inventory.supplier.api.support.ResourceDownloadException;

import java.util.Optional;

public interface SupplierProvider {

    Optional<FeedData> download() throws ResourceDownloadException;
}
