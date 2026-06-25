package pl.commercelink.inventory.supplier.api;

import pl.commercelink.provider.api.ProviderDescriptor;
import pl.commercelink.provider.api.ProviderField;

import java.util.List;

public interface SupplierProviderDescriptor extends ProviderDescriptor<SupplierProvider> {

    SupplierInfo supplierInfo();

    FeedFormat feedFormat();

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
