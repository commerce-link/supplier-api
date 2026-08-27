package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SupplierPurchaseRequestTest {

    private static final List<SupplierOrderLine> LINES = List.of(new SupplierOrderLine("SKU-1", "5900000000001", "MFN-1", 1));

    @Test
    void oldConstructorsLeaveOptionsEmptyNotNull() {
        assertEquals(Map.of(), new SupplierPurchaseRequest("ref", LINES).options());
        assertEquals(Map.of(), new SupplierPurchaseRequest("ref", LINES, "addr").options());
        assertEquals(Map.of(), new SupplierPurchaseRequest("ref", LINES, "addr", null).options());
    }

    @Test
    void optionsAreCopiedAndUnmodifiable() {
        Map<String, String> chosen = new HashMap<>(Map.of("deliveryMethod", "DPD Kurier"));
        SupplierPurchaseRequest request = new SupplierPurchaseRequest("ref", LINES, null, chosen);
        chosen.put("deliveryMethod", "changed");

        assertEquals("DPD Kurier", request.options().get("deliveryMethod"));
        assertThrows(UnsupportedOperationException.class, () -> request.options().put("x", "y"));
    }
}
