package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierProviderDefaultsTest {

    @Test
    void confirmedOrderIdDefaultsToEmpty() {
        // given
        SupplierProvider provider = () -> Optional.empty();

        // when / then
        assertTrue(provider.confirmedOrderId("ref-1").isEmpty());
    }

    @Test
    void orderTrackingIsUnsupportedByDefault() {
        // given
        SupplierProvider provider = () -> Optional.empty();

        // when / then
        assertFalse(provider.supportsOrderTracking());
    }

    @Test
    void trackOrderDefaultsToEmpty() {
        // given
        SupplierProvider provider = () -> Optional.empty();

        // when / then
        assertTrue(provider.trackOrder(new SupplierOrderLookup("SP-1", "ref-1")).isEmpty());
    }

    @Test
    void pickupPointDropshipIsUnsupportedByDefault() {
        // given
        SupplierProvider provider = () -> Optional.empty();

        // when / then
        assertFalse(provider.supportsPickupPointDropship());
    }

    @Test
    void providersDeclareNoOrderOptionsByDefault() {
        // given
        SupplierProvider provider = () -> Optional.empty();

        // when / then
        assertTrue(provider.orderOptions(SupplierOrderOptionsContext.warehouse()).isEmpty());
        assertTrue(provider.orderOptions(SupplierOrderOptionsContext.dropship(null)).isEmpty());
    }

    @Test
    void deferredConfirmationIsUnsupportedByDefault() {
        // given
        SupplierProvider provider = () -> Optional.empty();

        // when / then
        assertFalse(provider.supportsDeferredConfirmation());
        assertThrows(UnsupportedOperationException.class, () -> provider.completePlacedOrder(
                new SupplierPurchaseRequest("ref", List.of(new SupplierOrderLine("S", "E", "M", 1)))));
        assertThrows(UnsupportedOperationException.class, () -> provider.completePlacedDropshipOrder(null));
    }
}
