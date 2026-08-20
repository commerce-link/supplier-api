package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierOrderResultTest {

    @Test
    void legacyConstructorDefaultsToNotProvisional() {
        // when
        SupplierOrderResult result = new SupplierOrderResult("22", 10.0, "PLN", List.of());

        // then
        assertFalse(result.provisional());
    }

    @Test
    void carriesProvisionalFlag() {
        // when
        SupplierOrderResult result = new SupplierOrderResult("22", 10.0, "PLN", List.of(), true);

        // then
        assertTrue(result.provisional());
    }
}
