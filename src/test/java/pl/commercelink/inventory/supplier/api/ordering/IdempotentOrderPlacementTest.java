package pl.commercelink.inventory.supplier.api.ordering;

import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierPurchaseRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotentOrderPlacementTest {

    private static final SupplierPurchaseRequest REQUEST = new SupplierPurchaseRequest(
            "ref-1", List.of(new SupplierOrderLine("SKU-A", "4006381333931", "MFN-A", 2)));

    private static class TestPlacement extends IdempotentOrderPlacement<String, String> {

        private final List<String> callLog = new ArrayList<>();
        private String existingOrder;
        private String placedOrder = "PO-1";
        private RuntimeException translationFailure;
        private RuntimeException replayCheckFailure;
        private RuntimeException placementFailure;

        SupplierOrderResult place(SupplierPurchaseRequest request) {
            return placeIdempotently(request);
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
        protected String placeNewOrder(String clientOrderRef, List<String> lines) {
            callLog.add("place");
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
    void propagatesSupplierOrderExceptionUnchanged() {
        // given
        TestPlacement placement = new TestPlacement();
        placement.placementFailure = new SupplierOrderException("TestSupplier rejected purchase order: no stock");

        // when
        SupplierOrderException exception = assertThrows(SupplierOrderException.class,
                () -> placement.place(REQUEST));

        // then
        assertEquals("TestSupplier rejected purchase order: no stock", exception.getMessage());
    }
}
