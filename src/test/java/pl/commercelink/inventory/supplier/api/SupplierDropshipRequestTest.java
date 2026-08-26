package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SupplierDropshipRequestTest {

    private static final SupplierConsignee CONSIGNEE = new SupplierConsignee(null, "Jan", "Kowalski",
            "ul. Polna 1", "00-001", "Warszawa", "PL", "+48601234567", "jan@example.com");
    private static final List<SupplierOrderLine> LINES = List.of(new SupplierOrderLine("SKU-1", "5900000000001", "MFN-1", 1));

    @Test
    void shortConstructorsLeaveInstructionsAndPickupPointEmpty() {
        // when
        SupplierDropshipRequest three = new SupplierDropshipRequest("ref-1", LINES, CONSIGNEE);
        SupplierDropshipRequest four = new SupplierDropshipRequest("ref-1", LINES, CONSIGNEE, "note");

        // then
        assertNull(three.deliveryInstructions());
        assertNull(three.pickupPoint());
        assertEquals("note", four.deliveryInstructions());
        assertNull(four.pickupPoint());
    }

    @Test
    void fullConstructorCarriesThePickupPoint() {
        // given
        SupplierPickupPoint point = new SupplierPickupPoint("InPost", "WAW04A", null, null, null, null);

        // when
        SupplierDropshipRequest request = new SupplierDropshipRequest("ref-1", LINES, CONSIGNEE, null, point);

        // then
        assertEquals(point, request.pickupPoint());
    }
}
