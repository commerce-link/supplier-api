package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierPickupPointTest {

    @Test
    void acceptsCarrierAndCodeOnly() {
        // when
        SupplierPickupPoint point = new SupplierPickupPoint(" InPost ", " WAW04A ", null, null, null, null);

        // then
        assertEquals("InPost", point.carrier());
        assertEquals("WAW04A", point.code());
        assertNull(point.name());
        assertNull(point.streetAndNumber());
    }

    @Test
    void trimsOptionalAddressFieldsToNull() {
        // when
        SupplierPickupPoint point = new SupplierPickupPoint("DPD", "PL12345", " ", " ul. Polna 1 ", "", "Warszawa");

        // then
        assertNull(point.name());
        assertEquals("ul. Polna 1", point.streetAndNumber());
        assertNull(point.postalCode());
        assertEquals("Warszawa", point.city());
    }

    @Test
    void rejectsMissingCarrier() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierPickupPoint(" ", "WAW04A", null, null, null, null));
    }

    @Test
    void rejectsMissingCode() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierPickupPoint("InPost", null, null, null, null, null));
    }
}
