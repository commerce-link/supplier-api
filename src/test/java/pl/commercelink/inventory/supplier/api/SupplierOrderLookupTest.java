package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierOrderLookupTest {

    @Test
    void rejectsLookupWithoutAnyIdentifier() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new SupplierOrderLookup(" ", null));
        assertThrows(IllegalArgumentException.class, () -> new SupplierOrderLookup(null, ""));
    }

    @Test
    void acceptsEitherIdentifierAlone() {
        // when / then
        assertDoesNotThrow(() -> new SupplierOrderLookup("SP-1", null));
        assertDoesNotThrow(() -> new SupplierOrderLookup(null, "ref-1"));
    }

    @Test
    void trimsIdentifiers() {
        // when
        SupplierOrderLookup lookup = new SupplierOrderLookup(" SP-1 ", " ref-1 ");

        // then
        assertEquals("SP-1", lookup.externalOrderId());
        assertEquals("ref-1", lookup.clientOrderRef());
    }
}
