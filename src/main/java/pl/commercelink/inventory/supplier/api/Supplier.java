package pl.commercelink.inventory.supplier.api;

import pl.commercelink.inventory.supplier.api.support.ResourceDownloadException;

import java.util.Optional;

/**
 * Config-bound runtime object produced by {@link SupplierDescriptor#create(java.util.Map)}.
 * Equivalent to ShippingProvider / PaymentProvider in the other provider domains.
 */
public interface Supplier {

    Optional<FeedData> download() throws ResourceDownloadException;
}
