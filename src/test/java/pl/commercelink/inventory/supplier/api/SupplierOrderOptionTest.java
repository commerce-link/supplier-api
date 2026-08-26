package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SupplierOrderOptionTest {

    private static final SupplierOrderOptionChoice DPD = new SupplierOrderOptionChoice("DPD Kurier", "DPD Kurier", "max 30 kg");
    private static final SupplierOrderOptionChoice DHL = new SupplierOrderOptionChoice("DHL Kurier", "DHL Kurier", null);

    @Test
    void keepsChoicesImmutableAndFindsThemByValue() {
        SupplierOrderOption option = new SupplierOrderOption("deliveryMethod", "Delivery method", List.of(DPD, DHL), "DPD Kurier", true);

        assertEquals(DHL, option.choice("DHL Kurier").orElseThrow());
        assertTrue(option.choice("UPS").isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> option.choices().add(DPD));
    }

    @Test
    void rejectsBlankKeyOrLabel() {
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierOrderOption(" ", "Delivery method", List.of(DPD), null, true));
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierOrderOption("deliveryMethod", "", List.of(DPD), null, true));
    }

    @Test
    void rejectsDuplicateOrBlankChoiceValues() {
        assertThrows(IllegalArgumentException.class, () -> new SupplierOrderOption("k", "L",
                List.of(DPD, new SupplierOrderOptionChoice("DPD Kurier", "again", null)), null, true));
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierOrderOptionChoice("", "Blank", null));
    }

    @Test
    void defaultValueMustBeOneOfTheChoices() {
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierOrderOption("k", "L", List.of(DPD), "UPS", true));
        assertNull(new SupplierOrderOption("k", "L", List.of(DPD), null, false).defaultValue());
    }

    @Test
    void contextFactoriesDescribeTheOrder() {
        SupplierPickupPoint point = new SupplierPickupPoint("InPost", "WAW04A", null, null, null, null);

        assertFalse(SupplierOrderOptionsContext.warehouse().dropship());
        assertNull(SupplierOrderOptionsContext.warehouse().pickupPoint());
        assertTrue(SupplierOrderOptionsContext.dropship(point).dropship());
        assertEquals(point, SupplierOrderOptionsContext.dropship(point).pickupPoint());
        assertTrue(SupplierOrderOptionsContext.dropship(null).dropship());
    }
}
