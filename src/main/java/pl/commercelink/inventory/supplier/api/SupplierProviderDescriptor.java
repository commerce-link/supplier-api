package pl.commercelink.inventory.supplier.api;

import pl.commercelink.provider.api.ProviderDescriptor;
import pl.commercelink.provider.api.ProviderField;

import java.util.List;

/**
 * Supplier SPI. A descriptor is config-independent metadata (supplierInfo, feedFormat,
 * configurationFields) plus a factory method create(config) that returns a config-bound
 * {@link SupplierProvider}. Mirrors ShippingProviderDescriptor / PaymentProviderDescriptor.
 */
public interface SupplierProviderDescriptor extends ProviderDescriptor<SupplierProvider> {

    /** Config-independent metadata used for catalog listing and shipping/accuracy data. */
    SupplierInfo supplierInfo();

    /** Config-independent feed parsing specification (CSV or XML). */
    FeedFormat feedFormat();

    /**
     * Defaults to no fields so suppliers with a fixed/no-secret feed need not override it
     * (provider-api declares this abstract; suppliers historically defaulted to empty).
     */
    @Override
    default List<ProviderField> configurationFields() {
        return List.of();
    }

    @Override
    default String name() {
        return supplierInfo().name();
    }

    @Override
    default String displayName() {
        return supplierInfo().name();
    }
}
