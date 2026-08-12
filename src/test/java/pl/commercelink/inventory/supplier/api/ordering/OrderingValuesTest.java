package pl.commercelink.inventory.supplier.api.ordering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderingValuesTest {

    @Test
    void parsesPlainStockQuantity() {
        // when / then
        assertEquals(30, OrderingValues.parseStockQuantity("30"));
    }

    @Test
    void parsesStockQuantityWithPlusSuffix() {
        // when / then
        assertEquals(30, OrderingValues.parseStockQuantity("30+"));
    }

    @Test
    void parsesStockQuantityWithGroupingSeparators() {
        // when / then
        assertEquals(1000, OrderingValues.parseStockQuantity("1 000+"));
        assertEquals(1000, OrderingValues.parseStockQuantity("1,000"));
        assertEquals(1000, OrderingValues.parseStockQuantity("1 000"));
    }

    @Test
    void treatsNonNumericStockAsZero() {
        // when / then
        assertEquals(0, OrderingValues.parseStockQuantity("N/A"));
        assertEquals(0, OrderingValues.parseStockQuantity(""));
        assertEquals(0, OrderingValues.parseStockQuantity(null));
    }

    @Test
    void treatsOverflowingStockAsZero() {
        // when / then
        assertEquals(0, OrderingValues.parseStockQuantity("99999999999999999999"));
    }

    @Test
    void keepsProvidedCurrency() {
        // when / then
        assertEquals("EUR", OrderingValues.currencyOrDefault("EUR", "PLN"));
    }

    @Test
    void fallsBackToDefaultCurrencyWhenMissing() {
        // when / then
        assertEquals("PLN", OrderingValues.currencyOrDefault(null, "PLN"));
        assertEquals("PLN", OrderingValues.currencyOrDefault(" ", "PLN"));
    }

    @Test
    void encodesPathSegmentWithSlashesAndSpaces() {
        // when / then
        assertEquals("ORD%2F2026%201", OrderingValues.encodePathSegment("ORD/2026 1"));
    }
}
