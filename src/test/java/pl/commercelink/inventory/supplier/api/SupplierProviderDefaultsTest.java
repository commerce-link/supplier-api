package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierProviderDefaultsTest {

    @Test
    void confirmedOrderIdDefaultsToEmpty() {
        // given
        SupplierProvider provider = () -> Optional.empty();

        // when / then
        assertTrue(provider.confirmedOrderId("ref-1").isEmpty());
    }
}
