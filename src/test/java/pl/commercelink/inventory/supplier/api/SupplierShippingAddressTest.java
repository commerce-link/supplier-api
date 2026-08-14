package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierShippingAddressTest {

    @Test
    void normalisesCountryToUpperCase() {
        SupplierShippingAddress address = address("pl");
        assertEquals("PL", address.country());
    }

    @Test
    void rejectsNonAlpha2Country() {
        assertThrows(IllegalArgumentException.class, () -> address("POL"));
    }

    @Test
    void rejectsBlankStreet() {
        assertThrows(IllegalArgumentException.class, () -> new SupplierShippingAddress(
                "ACME sp. z o.o.", null, null, " ", "00-001", "Warszawa", "PL", null, null));
    }

    @Test
    void rejectsBlankPostalCodeCityAndCountry() {
        assertThrows(IllegalArgumentException.class, () -> new SupplierShippingAddress(
                "ACME sp. z o.o.", null, null, "Prosta 1", null, "Warszawa", "PL", null, null));
        assertThrows(IllegalArgumentException.class, () -> new SupplierShippingAddress(
                "ACME sp. z o.o.", null, null, "Prosta 1", "00-001", "", "PL", null, null));
        assertThrows(IllegalArgumentException.class, () -> new SupplierShippingAddress(
                "ACME sp. z o.o.", null, null, "Prosta 1", "00-001", "Warszawa", null, null, null));
    }

    @Test
    void requiresCompanyOrFullPersonName() {
        assertThrows(IllegalArgumentException.class, () -> new SupplierShippingAddress(
                null, "Jan", null, "Prosta 1", "00-001", "Warszawa", "PL", null, null));
    }

    @Test
    void acceptsPersonWithoutCompany() {
        SupplierShippingAddress address = new SupplierShippingAddress(
                null, "Jan", "Kowalski", "Prosta 1", "00-001", "Warszawa", "PL", null, null);
        assertEquals("Kowalski", address.lastName());
    }

    private static SupplierShippingAddress address(String country) {
        return new SupplierShippingAddress("ACME sp. z o.o.", null, null,
                "Prosta 1", "00-001", "Warszawa", country, "+48123456789", "magazyn@acme.pl");
    }
}
