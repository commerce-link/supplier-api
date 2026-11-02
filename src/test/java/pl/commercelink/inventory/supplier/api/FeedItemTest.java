package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedItemTest {

    @Test
    void feedItemWithOnlyRequiredValuesGetsDefaults() {
        // given
        FeedItem feedItem = new FeedItem() {
            public String getEan() { return "5901234123457"; }
            public String getMfn() { return "MFN-1"; }
            public String getBrand() { return "ASUS"; }
            public String getName() { return "GeForce RTX"; }
            public String getCategory() { return "Komputery > Karty graficzne"; }
            public double getNetPrice() { return 99.99; }
            public String getCurrency() { return "PLN"; }
            public int getQty() { return 3; }
        };

        // then
        assertEquals(0, feedItem.getLeadTimeDays());
        assertTrue(feedItem.isSellable());
        assertFalse(feedItem.isInStock());
        assertFalse(feedItem.isInDelivery());
        assertNull(feedItem.getSku());
        assertNull(feedItem.getNetWeightInGrams());
        assertNull(feedItem.getGrossWeightInGrams());
    }
}
