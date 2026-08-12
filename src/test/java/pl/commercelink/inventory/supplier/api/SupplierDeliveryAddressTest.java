package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierDeliveryAddressTest {

    @Test
    void labelJoinsStreetPostalCodeCityAndCountry() {
        // given
        SupplierDeliveryAddress address = new SupplierDeliveryAddress(
                "17200617", "ul. Łobzowska 22/1", "Kraków", "31-140", "PL");

        // when / then
        assertEquals("ul. Łobzowska 22/1, 31-140 Kraków, PL", address.label());
    }

    @Test
    void labelSkipsMissingParts() {
        // given
        SupplierDeliveryAddress address = new SupplierDeliveryAddress(
                "7", "Main Street 1", null, "  ", "DE");

        // when / then
        assertEquals("Main Street 1, DE", address.label());
    }

    @Test
    void labelFallsBackToIdWhenNothingElseIsKnown() {
        // given
        SupplierDeliveryAddress address = new SupplierDeliveryAddress("7", null, null, null, null);

        // when / then
        assertEquals("7", address.label());
    }

    @Test
    void blankIdIsRejected() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierDeliveryAddress(" ", "Main Street 1", "Berlin", "10115", "DE"));
    }

    @Test
    void nullIdIsRejected() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierDeliveryAddress(null, "Main Street 1", "Berlin", "10115", "DE"));
    }
}
