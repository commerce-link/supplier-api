package pl.commercelink.inventory.supplier.api;

import pl.commercelink.inventory.supplier.api.support.ResourceDownloadException;

import java.util.List;
import java.util.Optional;

public interface SupplierDescriptor {
    Optional<FeedData> download(String secret) throws ResourceDownloadException;

    FeedFormat feedFormat();
    SupplierInfo supplierInfo();

    default List<SupplierConfigField> configurationFields() {
        return List.of();
    }

    default boolean supports(String supplierName) {
        return supplierInfo().name().equalsIgnoreCase(supplierName);
    }
}
