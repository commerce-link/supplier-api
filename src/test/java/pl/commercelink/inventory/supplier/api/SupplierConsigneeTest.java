package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierConsigneeTest {

    @Test
    void acceptsNaturalPersonWithFullContactData() {
        // when
        SupplierConsignee consignee = new SupplierConsignee(null, "Jan", "Kowalski",
                "ul. Polna 1", "00-001", "Warszawa", "PL", "+48601234567", "jan@example.com");

        // then
        assertEquals("Jan Kowalski", consignee.recipientName());
    }

    @Test
    void acceptsCompanyWithoutPersonalName() {
        // when
        SupplierConsignee consignee = new SupplierConsignee("ACME Sp. z o.o.", null, null,
                "ul. Polna 1", "00-001", "Warszawa", "PL", "+48601234567", "biuro@example.com");

        // then
        assertEquals("ACME Sp. z o.o.", consignee.recipientName());
    }

    @Test
    void rejectsMissingPhone() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new SupplierConsignee(null, "Jan", "Kowalski",
                "ul. Polna 1", "00-001", "Warszawa", "PL", " ", "jan@example.com"));
    }

    @Test
    void rejectsMissingEmail() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new SupplierConsignee(null, "Jan", "Kowalski",
                "ul. Polna 1", "00-001", "Warszawa", "PL", "+48601234567", null));
    }

    @Test
    void rejectsMissingRecipientIdentity() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new SupplierConsignee(null, "Jan", null,
                "ul. Polna 1", "00-001", "Warszawa", "PL", "+48601234567", "jan@example.com"));
    }

    @Test
    void rejectsMissingAddressParts() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new SupplierConsignee(null, "Jan", "Kowalski",
                null, "00-001", "Warszawa", "PL", "+48601234567", "jan@example.com"));
        assertThrows(IllegalArgumentException.class, () -> new SupplierConsignee(null, "Jan", "Kowalski",
                "ul. Polna 1", " ", "Warszawa", "PL", "+48601234567", "jan@example.com"));
        assertThrows(IllegalArgumentException.class, () -> new SupplierConsignee(null, "Jan", "Kowalski",
                "ul. Polna 1", "00-001", null, "PL", "+48601234567", "jan@example.com"));
    }

    @Test
    void rejectsCountryOutsideIsoAlpha2() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new SupplierConsignee(null, "Jan", "Kowalski",
                "ul. Polna 1", "00-001", "Warszawa", "pl", "+48601234567", "jan@example.com"));
        assertThrows(IllegalArgumentException.class, () -> new SupplierConsignee(null, "Jan", "Kowalski",
                "ul. Polna 1", "00-001", "Warszawa", "POL", "+48601234567", "jan@example.com"));
        assertThrows(IllegalArgumentException.class, () -> new SupplierConsignee(null, "Jan", "Kowalski",
                "ul. Polna 1", "00-001", "Warszawa", "XX", "+48601234567", "jan@example.com"));
    }
}
