package pl.commercelink.inventory.supplier.api.ordering;

import pl.commercelink.inventory.supplier.api.SupplierOrderLine;

import java.util.Optional;

public interface ProductCodeResolver {

    Optional<SupplierProductCode> resolve(SupplierOrderLine line);
}
