package pl.commercelink.inventory.supplier.api.ordering;

import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.SupplierConsignee;
import pl.commercelink.inventory.supplier.api.SupplierDropshipRequest;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderOutcomeUnknownException;
import pl.commercelink.inventory.supplier.api.SupplierOrderRejectedException;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierPickupPoint;
import pl.commercelink.inventory.supplier.api.SupplierPurchaseRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotentOrderPlacementTest {

    private static final SupplierPurchaseRequest REQUEST = new SupplierPurchaseRequest(
            "ref-1", List.of(new SupplierOrderLine("SKU-A", "4006381333931", "MFN-A", 2)));

    private static final SupplierConsignee CONSIGNEE = new SupplierConsignee(null, "Jan", "Kowalski",
            "ul. Polna 1", "00-001", "Warszawa", "PL", "+48601234567", "jan.kowalski@example.com");
    private static final SupplierDropshipRequest DROPSHIP_REQUEST = new SupplierDropshipRequest(
            "ref-ds-1", REQUEST.lines(), CONSIGNEE);

    private static final SupplierPickupPoint PICKUP_POINT =
            new SupplierPickupPoint("InPost", "WAW04A", null, null, null, null);
    private static final SupplierDropshipRequest PICKUP_REQUEST = new SupplierDropshipRequest(
            "ref-ds-pp", REQUEST.lines(), CONSIGNEE, null, PICKUP_POINT);

    private static class TestPlacement extends IdempotentOrderPlacement<String, String> {

        private final List<String> callLog = new ArrayList<>();
        private String existingOrder;
        private String placedOrder = "PO-1";
        private RuntimeException translationFailure;
        private RuntimeException replayCheckFailure;
        private RuntimeException placementFailure;
        private SupplierPurchaseRequest placedWith;
        private SupplierDropshipRequest dropshipPlacedWith;
        private boolean acceptsPickupPoint;
        private String existingDropshipOrder;

        SupplierOrderResult place(SupplierPurchaseRequest request) {
            return placeIdempotently(request);
        }

        SupplierOrderResult placeDropship(SupplierDropshipRequest request) {
            return placeDropshipIdempotently(request);
        }

        @Override
        protected String supplierName() {
            return "TestSupplier";
        }

        @Override
        protected String toSupplierLine(SupplierOrderLine line) {
            callLog.add("translate");
            if (translationFailure != null) throw translationFailure;
            return line.ean();
        }

        @Override
        protected Optional<String> findExistingOrder(String clientOrderRef) {
            callLog.add("findExisting");
            if (replayCheckFailure != null) throw replayCheckFailure;
            return Optional.ofNullable(existingOrder);
        }

        @Override
        protected boolean acceptsPickupPoint() {
            return acceptsPickupPoint;
        }

        @Override
        protected Optional<String> findExistingDropshipOrder(String clientOrderRef) {
            callLog.add("findExistingDropship");
            if (replayCheckFailure != null) throw replayCheckFailure;
            return Optional.ofNullable(existingDropshipOrder != null ? existingDropshipOrder : existingOrder);
        }

        @Override
        protected String placeNewOrder(SupplierPurchaseRequest request, List<String> lines) {
            callLog.add("place");
            placedWith = request;
            if (placementFailure != null) throw placementFailure;
            return placedOrder;
        }

        @Override
        protected String placeNewDropshipOrder(SupplierDropshipRequest request, List<String> lines) {
            callLog.add("placeDropship");
            dropshipPlacedWith = request;
            if (placementFailure != null) throw placementFailure;
            return placedOrder;
        }

        @Override
        protected String externalOrderId(String order) {
            return order.isBlank() ? null : order;
        }

        @Override
        protected SupplierOrderResult toResult(String order, SupplierPurchaseRequest request) {
            callLog.add("toResult:" + order);
            return new SupplierOrderResult(order, 100.0, "PLN", List.of());
        }
    }

    @Test
    void placesOrderWhenNoExistingOrderFound() {
        // given
        TestPlacement placement = new TestPlacement();

        // when
        SupplierOrderResult result = placement.place(REQUEST);

        // then
        assertEquals("PO-1", result.externalOrderId());
        assertEquals(List.of("translate", "findExisting", "place", "toResult:PO-1"), placement.callLog);
    }

    @Test
    void handsWholeRequestIncludingDeliveryAddressToPlacement() {
        // given
        TestPlacement placement = new TestPlacement();
        SupplierPurchaseRequest request = new SupplierPurchaseRequest(
                "ref-addr", REQUEST.lines(), "17200617");

        // when
        placement.place(request);

        // then
        assertEquals("17200617", placement.placedWith.deliveryAddressId());
        assertEquals("ref-addr", placement.placedWith.clientOrderRef());
    }

    @Test
    void returnsExistingOrderWithoutPlacingSecond() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.existingOrder = "PO-EXISTING";

        // when
        SupplierOrderResult result = placement.place(REQUEST);

        // then
        assertEquals("PO-EXISTING", result.externalOrderId());
        assertEquals(List.of("translate", "findExisting", "toResult:PO-EXISTING"), placement.callLog);
    }

    @Test
    void throwsOnBlankRefBeforeAnyHook() {
        // given
        TestPlacement placement = new TestPlacement();

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> placement.place(new SupplierPurchaseRequest(" ", REQUEST.lines())));
        assertTrue(placement.callLog.isEmpty());
    }

    @Test
    void throwsOnNullRefBeforeAnyHook() {
        // given
        TestPlacement placement = new TestPlacement();

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> placement.place(new SupplierPurchaseRequest(null, REQUEST.lines())));
        assertTrue(placement.callLog.isEmpty());
    }

    @Test
    void translatesLinesBeforeReplayCheck() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.translationFailure = new SupplierOrderException("No code found");

        // when / then
        assertThrows(SupplierOrderException.class, () -> placement.place(REQUEST));
        assertEquals(List.of("translate"), placement.callLog);
    }

    @Test
    void throwsWhenPlacedOrderHasNoOrderId() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placedOrder = " ";

        // when
        SupplierOrderException exception = assertThrows(SupplierOrderException.class,
                () -> placement.place(REQUEST));

        // then
        assertEquals("TestSupplier returned no purchase order id for ref ref-1", exception.getMessage());
    }

    @Test
    void throwsWhenPlacedOrderIsNull() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placedOrder = null;

        // when / then
        assertThrows(SupplierOrderException.class, () -> placement.place(REQUEST));
    }

    @Test
    void wrapsRawRuntimeExceptionsInSupplierOrderException() {
        // given
        TestPlacement placement = new TestPlacement();
        IllegalStateException cause = new IllegalStateException("connection reset");
        placement.replayCheckFailure = cause;

        // when
        SupplierOrderException exception = assertThrows(SupplierOrderException.class,
                () -> placement.place(REQUEST));

        // then
        assertEquals("TestSupplier replay check failed", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    void placementRuntimeFailureSurfacesAsOutcomeUnknown() {
        // given
        TestPlacement placement = new TestPlacement();
        IllegalStateException cause = new IllegalStateException("boom");
        placement.placementFailure = cause;

        // when
        SupplierOrderOutcomeUnknownException exception = assertThrows(SupplierOrderOutcomeUnknownException.class,
                () -> placement.place(REQUEST));

        // then
        assertEquals(cause, exception.getCause());
    }

    @Test
    void placementPlainSupplierOrderExceptionSurfacesAsOutcomeUnknown() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placementFailure = new SupplierOrderException("supplier said something odd");

        // when
        SupplierOrderOutcomeUnknownException exception = assertThrows(SupplierOrderOutcomeUnknownException.class,
                () -> placement.place(REQUEST));

        // then
        assertTrue(exception.getMessage().contains("supplier said something odd"));
    }

    @Test
    void placementRejectionPassesThrough() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placementFailure = new SupplierOrderRejectedException("no stock");

        // when / then
        assertThrows(SupplierOrderRejectedException.class, () -> placement.place(REQUEST));
    }

    @Test
    void missingOrderIdSurfacesAsOutcomeUnknown() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placedOrder = " ";

        // when
        SupplierOrderOutcomeUnknownException exception = assertThrows(SupplierOrderOutcomeUnknownException.class,
                () -> placement.place(REQUEST));

        // then
        assertTrue(exception.getMessage().contains("returned no purchase order id"));
    }

    @Test
    void replayCheckFailureStaysPlainSupplierOrderException() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.replayCheckFailure = new IllegalStateException("api down");

        // when
        SupplierOrderException exception = assertThrows(SupplierOrderException.class,
                () -> placement.place(REQUEST));

        // then
        assertFalse(exception instanceof SupplierOrderRejectedException);
        assertFalse(exception instanceof SupplierOrderOutcomeUnknownException);
    }

    @Test
    void findPlacedOrderMapsExistingOrder() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.existingOrder = "PO-EXISTING";

        // when
        Optional<SupplierOrderResult> result = placement.findPlacedOrder(REQUEST);

        // then
        assertTrue(result.isPresent());
        assertEquals("PO-EXISTING", result.orElseThrow().externalOrderId());
        // The regular lookup already found the order, so the dropship lookup is skipped
        // (Optional#or short-circuits) — proves reconcile doesn't pay for a probe it doesn't need.
        assertEquals(List.of("findExisting", "toResult:PO-EXISTING"), placement.callLog);
    }

    @Test
    void findPlacedOrderReturnsEmptyForUnknownRef() {
        // given
        TestPlacement placement = new TestPlacement();

        // when
        Optional<SupplierOrderResult> result = placement.findPlacedOrder(REQUEST);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void placesDropshipOrderWhenNoExistingOrderFound() {
        // given
        TestPlacement placement = new TestPlacement();

        // when
        SupplierOrderResult result = placement.placeDropship(DROPSHIP_REQUEST);

        // then
        assertEquals("PO-1", result.externalOrderId());
        assertEquals(List.of("translate", "findExistingDropship", "placeDropship", "toResult:PO-1"), placement.callLog);
        assertEquals(CONSIGNEE, placement.dropshipPlacedWith.consignee());
    }

    @Test
    void dropshipRetryReturnsExistingOrderWithoutPlacingSecond() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.existingOrder = "PO-EXISTING";

        // when
        SupplierOrderResult result = placement.placeDropship(DROPSHIP_REQUEST);

        // then
        assertEquals("PO-EXISTING", result.externalOrderId());
        assertEquals(List.of("translate", "findExistingDropship", "toResult:PO-EXISTING"), placement.callLog);
    }

    @Test
    void pickupPointRequestIsRejectedBeforeAnyHookWhenUnsupported() {
        // given
        TestPlacement placement = new TestPlacement();

        // when / then
        assertThrows(SupplierOrderRejectedException.class, () -> placement.placeDropship(PICKUP_REQUEST));
        assertTrue(placement.callLog.isEmpty());
    }

    @Test
    void pickupPointRequestReachesPlacementWhenSupported() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.acceptsPickupPoint = true;

        // when
        SupplierOrderResult result = placement.placeDropship(PICKUP_REQUEST);

        // then
        assertEquals("PO-1", result.externalOrderId());
        assertEquals(PICKUP_POINT, placement.dropshipPlacedWith.pickupPoint());
    }

    @Test
    void dropshipPlacementRuntimeFailureSurfacesAsOutcomeUnknown() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placementFailure = new IllegalStateException("connection reset");

        // when / then
        SupplierOrderOutcomeUnknownException exception = assertThrows(SupplierOrderOutcomeUnknownException.class,
                () -> placement.placeDropship(DROPSHIP_REQUEST));
        assertTrue(exception.getMessage().contains("dropship order placement failed"));
    }

    @Test
    void dropshipPlacementPlainSupplierOrderExceptionSurfacesAsOutcomeUnknown() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placementFailure = new SupplierOrderException("SOAP timeout");

        // when / then
        assertThrows(SupplierOrderOutcomeUnknownException.class, () -> placement.placeDropship(DROPSHIP_REQUEST));
    }

    @Test
    void dropshipPlacementRejectionPassesThrough() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placementFailure = new SupplierOrderRejectedException("no stock");

        // when / then
        assertThrows(SupplierOrderRejectedException.class, () -> placement.placeDropship(DROPSHIP_REQUEST));
    }

    @Test
    void missingDropshipOrderIdSurfacesAsOutcomeUnknown() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placedOrder = " ";

        // when / then
        assertThrows(SupplierOrderOutcomeUnknownException.class, () -> placement.placeDropship(DROPSHIP_REQUEST));
    }

    @Test
    void dropshipReplayCheckFailureStaysPlainSupplierOrderException() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.replayCheckFailure = new IllegalStateException("list unavailable");

        // when / then
        SupplierOrderException exception = assertThrows(SupplierOrderException.class,
                () -> placement.placeDropship(DROPSHIP_REQUEST));
        assertFalse(exception instanceof SupplierOrderRejectedException);
        assertFalse(exception instanceof SupplierOrderOutcomeUnknownException);
    }

    @Test
    void findPlacedOrderFallsBackToDropshipLookup() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.existingDropshipOrder = "DS-77";

        // when
        Optional<SupplierOrderResult> result = placement.findPlacedOrder(REQUEST);

        // then
        assertEquals("DS-77", result.orElseThrow().externalOrderId());
        assertEquals(List.of("findExisting", "findExistingDropship", "toResult:DS-77"), placement.callLog);
    }

    @Test
    void dropshipBlankRefThrowsBeforeAnyHook() {
        // given
        TestPlacement placement = new TestPlacement();

        // when / then
        assertThrows(SupplierOrderException.class, () -> placement.placeDropship(
                new SupplierDropshipRequest(" ", REQUEST.lines(), CONSIGNEE)));
        assertTrue(placement.callLog.isEmpty());
    }

    @Test
    void dropshipNullConsigneeThrowsBeforeAnyHook() {
        // given
        TestPlacement placement = new TestPlacement();

        // when / then
        assertThrows(SupplierOrderException.class, () -> placement.placeDropship(
                new SupplierDropshipRequest("ref-ds-2", REQUEST.lines(), null)));
        assertTrue(placement.callLog.isEmpty());
    }

    @Test
    void dropshipWithoutPlacementOverrideSurfacesAsSupplierOrderException() {
        // given — a placement that never overrode the dropship hook
        IdempotentOrderPlacement<String, String> placement = new IdempotentOrderPlacement<>() {
            @Override protected String supplierName() { return "NoDropship"; }
            @Override protected String toSupplierLine(SupplierOrderLine line) { return line.ean(); }
            @Override protected Optional<String> findExistingOrder(String clientOrderRef) { return Optional.empty(); }
            @Override protected String placeNewOrder(SupplierPurchaseRequest request, List<String> lines) { return "PO-1"; }
            @Override protected String externalOrderId(String order) { return order; }
            @Override protected SupplierOrderResult toResult(String order, SupplierPurchaseRequest request) {
                return new SupplierOrderResult(order, 0, "PLN", List.of());
            }
        };

        // when
        SupplierOrderException e = assertThrows(SupplierOrderException.class,
                () -> placement.placeDropshipIdempotently(DROPSHIP_REQUEST));

        // then
        assertInstanceOf(UnsupportedOperationException.class, e.getCause());
    }
}
