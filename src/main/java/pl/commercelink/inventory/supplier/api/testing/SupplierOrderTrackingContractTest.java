package pl.commercelink.inventory.supplier.api.testing;

import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderLookup;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierOrderState;
import pl.commercelink.inventory.supplier.api.SupplierOrderTracking;
import pl.commercelink.inventory.supplier.api.SupplierParcel;
import pl.commercelink.inventory.supplier.api.SupplierProvider;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Executable order-tracking contract for {@link SupplierProvider} implementations; every
 * adapter answering {@code supportsOrderTracking() == true} ships a test class extending this
 * kit. Rules: unknown orders are empty, a lookup without identifiers is rejected before any
 * remote call, a freshly placed order is visible and not cancelled, shipped orders carry parcels
 * with carrier and tracking number, tracking never places orders, failures surface only as
 * {@link SupplierOrderException}. Documented in the supplier-api README under
 * "Order tracking contract".
 */
public abstract class SupplierOrderTrackingContractTest {

    /** Provider supporting tracking and able to place an order for {@link #sampleLines()}. */
    protected abstract SupplierProvider trackingProvider();

    /** Order lines known to the adapter's fixture. */
    protected abstract List<SupplierOrderLine> sampleLines();

    /** A client order reference never used before in this JVM. */
    protected abstract String uniqueClientOrderRef();

    /** Places an order (regular or dropship, whichever the adapter supports) so it can be tracked. */
    protected abstract SupplierOrderResult placeSampleOrder(SupplierProvider provider, String clientOrderRef);

    /** Makes the (fake) supplier report the order as shipped; {@code false} when not simulated. */
    protected boolean advanceToShipped(SupplierProvider provider, String clientOrderRef, String externalOrderId) {
        return false;
    }

    /** Provider whose backend fails every remote call with a raw transport error. */
    protected Optional<SupplierProvider> trackingProviderWithFailingBackend() {
        return Optional.empty();
    }

    /** Number of orders actually placed at the (fake) supplier since the test started, if observable. */
    protected OptionalInt remotePlacedOrders() {
        return OptionalInt.empty();
    }

    @Test
    void providerReportsTrackingSupport() {
        // when / then
        assertTrue(trackingProvider().supportsOrderTracking());
    }

    @Test
    void unknownOrderIsEmpty() {
        // given
        String ref = uniqueClientOrderRef();

        // when
        Optional<SupplierOrderTracking> tracking = trackingProvider()
                .trackOrder(new SupplierOrderLookup("no-such-order-" + ref, ref));

        // then
        assertTrue(tracking.isEmpty());
    }

    @Test
    void lookupWithoutAnyIdentifierIsRejected() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new SupplierOrderLookup(" ", null));
    }

    @Test
    void placedOrderIsVisibleAndNotCancelled() {
        // given
        SupplierProvider provider = trackingProvider();
        String ref = uniqueClientOrderRef();
        SupplierOrderResult placed = placeSampleOrder(provider, ref);

        // when
        Optional<SupplierOrderTracking> tracking = provider
                .trackOrder(new SupplierOrderLookup(placed.externalOrderId(), ref));

        // then
        assertTrue(tracking.isPresent());
        assertNotEquals(SupplierOrderState.CANCELLED, tracking.get().state());
    }

    @Test
    void shippedOrderCarriesParcelsWithCarrierAndTrackingNo() {
        // given
        SupplierProvider provider = trackingProvider();
        String ref = uniqueClientOrderRef();
        SupplierOrderResult placed = placeSampleOrder(provider, ref);
        assumeTrue(advanceToShipped(provider, ref, placed.externalOrderId()),
                "Adapter does not simulate shipment — hook not implemented");

        // when
        SupplierOrderTracking tracking = provider
                .trackOrder(new SupplierOrderLookup(placed.externalOrderId(), ref)).orElseThrow();

        // then
        assertTrue(tracking.state() == SupplierOrderState.SHIPPED
                || tracking.state() == SupplierOrderState.PARTIALLY_SHIPPED);
        assertFalse(tracking.parcels().isEmpty());
        for (SupplierParcel parcel : tracking.parcels()) {
            assertFalse(parcel.carrier().isBlank());
            assertFalse(parcel.trackingNo().isBlank());
        }
    }

    @Test
    void trackingDoesNotPlaceOrders() {
        // given
        SupplierProvider provider = trackingProvider();
        String ref = uniqueClientOrderRef();
        SupplierOrderResult placed = placeSampleOrder(provider, ref);
        assumeTrue(remotePlacedOrders().isPresent(), "Adapter does not expose placed order count");
        int before = remotePlacedOrders().getAsInt();

        // when
        provider.trackOrder(new SupplierOrderLookup(placed.externalOrderId(), ref));
        provider.trackOrder(new SupplierOrderLookup(null, ref));

        // then
        assertEquals(before, remotePlacedOrders().getAsInt());
    }

    @Test
    void failureSurfacesOnlyAsSupplierOrderException() {
        // given
        Optional<SupplierProvider> failing = trackingProviderWithFailingBackend();
        assumeTrue(failing.isPresent(), "Adapter does not simulate a failing backend — hook not implemented");

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> failing.get().trackOrder(new SupplierOrderLookup("SP-1", uniqueClientOrderRef())));
    }
}
