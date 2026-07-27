package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlItemTest {

    private static final SupplierInfo SUPPLIER = new SupplierInfo("SupplierX", SupplierType.Distributor, 7, "PL",
            new ShippingPolicy(new ShippingTerms(1, new ShippingCostPolicy.Free())));

    private XmlItem item(String ean, String mfn) {
        return new XmlItem() {
            @Override
            public String getEan() { return ean; }

            @Override
            public String getMfn() { return mfn; }

            @Override
            public String getBrand() { return "BrandX"; }

            @Override
            public String getName() { return "GeForce RTX"; }

            @Override
            public String getCategory() { return "GPU"; }

            @Override
            public double getNetPrice() { return 99.99; }

            @Override
            public int getQty() { return 3; }

            @Override
            public String getCurrency() { return "PLN"; }
        };
    }

    @Test
    void toParsedRowFallsBackToCategoryWhenRawCategoryMissing() {
        // when
        ParsedRow row = item("5901234123457", "MFN-1").toParsedRow(SUPPLIER);

        // then
        assertEquals("GPU", row.product().rawCategory());
    }

    @Test
    void toParsedRowCarriesItemAndProductFields() {
        // when
        ParsedRow row = item("5901234123457", "MFN-1").toParsedRow(SUPPLIER);

        // then
        assertEquals("5901234123457", row.item().ean());
        assertEquals("MFN-1", row.item().mfn());
        assertEquals(99.99, row.item().netPrice());
        assertEquals("PLN", row.item().currency());
        assertEquals(3, row.item().qty());
        assertEquals("SupplierX", row.item().supplier());
        assertEquals("BrandX", row.product().brand());
        assertEquals("GeForce RTX", row.product().name());
        assertEquals(7, row.product().dataAccuracyScore());
        assertNull(row.product().netWeightInGrams());
        assertNull(row.product().grossWeightInGrams());
    }

    @Test
    void toParsedRowUnifiesEanAndMfn() {
        // when
        ParsedRow row = item("0590123412345", "mfn 1").toParsedRow(SUPPLIER);

        // then
        assertEquals("590123412345", row.product().ean());
        assertEquals("MFN1", row.product().mfn());
    }

    @Test
    void defaultFlagsAreSellableNotInStockNotInDelivery() {
        // when
        XmlItem xmlItem = item("5901234123457", "MFN-1");

        // then
        assertTrue(xmlItem.isSellable());
        assertFalse(xmlItem.isInStock());
        assertFalse(xmlItem.isInDelivery());
    }

    @Test
    void toParsedRowUsesRawCategoryWhenPresent() {
        // given
        XmlItem withRawCategory = new XmlItem() {
            @Override
            public String getEan() { return "5901234123457"; }

            @Override
            public String getMfn() { return "MFN-1"; }

            @Override
            public String getBrand() { return "BrandX"; }

            @Override
            public String getName() { return "GeForce RTX"; }

            @Override
            public String getCategory() { return "GPU"; }

            @Override
            public double getNetPrice() { return 99.99; }

            @Override
            public int getQty() { return 3; }

            @Override
            public String getCurrency() { return "PLN"; }

            @Override
            public String getRawCategory() { return "Komputery > Karty graficzne"; }
        };

        // when
        ParsedRow row = withRawCategory.toParsedRow(SUPPLIER);

        // then
        assertEquals("Komputery > Karty graficzne", row.product().rawCategory());
    }
}
