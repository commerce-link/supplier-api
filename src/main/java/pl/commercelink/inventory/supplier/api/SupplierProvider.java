package pl.commercelink.inventory.supplier.api;

import pl.commercelink.inventory.supplier.api.support.ResourceDownloadException;

import java.util.Optional;

/**
 * Config-bound runtime object produced by {@link SupplierProviderDescriptor#create(java.util.Map)}.
 * Equivalent to ShippingProvider / PaymentProvider in the other provider domains.
 */
public interface SupplierProvider {

    Optional<FeedData> download() throws ResourceDownloadException;
}
