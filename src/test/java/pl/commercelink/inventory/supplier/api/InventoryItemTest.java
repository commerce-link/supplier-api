package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class InventoryItemTest {

    @Test
    void withSupplierReattributesTheRowKeepingEveryOtherField() {
        // given
        InventoryItem parsedByType = new InventoryItem("1234567890123", "MFN-1", 10.0, "PLN", 5, 1, "Kosatec",
                true, true, false, "0101");

        // when
        InventoryItem stamped = parsedByType.withSupplier("Kosatec-k7f3a9c2");

        // then
        assertEquals("Kosatec-k7f3a9c2", stamped.supplier());
        assertEquals("Kosatec-k7f3a9c2_1234567890123_MFN-1", stamped.uuid());
        assertEquals(parsedByType.netPrice(), stamped.netPrice());
        assertEquals(parsedByType.sku(), stamped.sku());
        assertSame(parsedByType, parsedByType.withSupplier("Kosatec"));
    }

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
