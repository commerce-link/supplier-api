package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryItemTest {

    @Test
    void withSkuAttachesRawSkuWithoutNormalization() {
        InventoryItem item = new InventoryItem("1234567890123", "MFN-1", 10.0, "PLN", 5, 1, "Elko")
                .withSku("0101");
        assertEquals("0101", item.sku());
        assertEquals("1234567890123", item.ean());
    }

    @Test
    void delegatingConstructorsLeaveSkuNull() {
        assertNull(new InventoryItem("1234567890123", "MFN-1", 10.0, "PLN", 5, 1, "Elko").sku());
        assertNull(new InventoryItem("1234567890123", "MFN-1", 10.0, "PLN", 5, 1, "Elko", false).sku());
        assertNull(new InventoryItem("1234567890123", "MFN-1", 10.0, "PLN", 5, 1, "Elko", true, true, false).sku());
    }

    @Test
    void currencyConversionAndEanRewritePreserveSku() {
        InventoryItem item = new InventoryItem("1234567890123", "MFN-1", 10.0, "PLN", 5, 1, "Elko").withSku("101");
        assertEquals("101", item.toLocalCurrency("EUR", 4.0).orElseThrow().sku());
        assertEquals("101", item.withEan("4006381333931").sku());
    }
}
