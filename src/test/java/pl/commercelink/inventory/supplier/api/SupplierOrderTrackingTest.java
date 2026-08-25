package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierOrderTrackingTest {

    @Test
    void requiresState() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new SupplierOrderTracking(null, List.of()));
    }

    @Test
    void nullParcelsBecomeEmptyList() {
        // when
        SupplierOrderTracking tracking = new SupplierOrderTracking(SupplierOrderState.PROCESSING, null);

        // then
        assertTrue(tracking.parcels().isEmpty());
    }

    @Test
    void parcelsAreCopiedAndImmutable() {
        // given
        List<SupplierParcel> parcels = new ArrayList<>(List.of(new SupplierParcel("DPD", "PKG-1", null, null, null)));

        // when
        SupplierOrderTracking tracking = new SupplierOrderTracking(SupplierOrderState.SHIPPED, parcels);
        parcels.clear();

        // then
        assertEquals(1, tracking.parcels().size());
        assertThrows(UnsupportedOperationException.class, () -> tracking.parcels().clear());
    }
}
