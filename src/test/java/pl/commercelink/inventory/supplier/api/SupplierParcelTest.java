package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierParcelTest {

    @Test
    void requiresCarrierAndTrackingNo() {
        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierParcel(" ", "PKG-1", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SupplierParcel("DPD", null, null, null, null));
    }

    @Test
    void nullLinesBecomeEmptyList() {
        // when
        SupplierParcel parcel = new SupplierParcel("DPD", "PKG-1", null, null, null);

        // then
        assertTrue(parcel.lines().isEmpty());
    }

    @Test
    void linesAreCopiedAndImmutable() {
        // given
        List<SupplierOrderLine> lines = new ArrayList<>(List.of(new SupplierOrderLine("sku", "5900000000001", "MFN", 1)));

        // when
        SupplierParcel parcel = new SupplierParcel("DPD", "PKG-1", null, null, lines);
        lines.clear();

        // then
        assertEquals(1, parcel.lines().size());
        assertThrows(UnsupportedOperationException.class, () -> parcel.lines().add(null));
    }

    @Test
    void trimsCarrierAndTrackingNo() {
        // when
        SupplierParcel parcel = new SupplierParcel(" DPD ", " PKG-1 ", " https://t/1 ", null, null);

        // then
        assertEquals("DPD", parcel.carrier());
        assertEquals("PKG-1", parcel.trackingNo());
        assertEquals("https://t/1", parcel.trackingUrl());
    }
}
