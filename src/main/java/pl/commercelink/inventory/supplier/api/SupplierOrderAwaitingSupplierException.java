package pl.commercelink.inventory.supplier.api;

import java.util.List;

/**
 * The order exists at the supplier under {@link #externalOrderId()}, but the supplier has not confirmed it yet - for
 * example the reservation is still being made line by line. Nothing was cancelled and nothing was bought: ask again
 * later through {@link SupplierProvider#completePlacedOrder}. Distinct from a rejection (nothing exists, retry is safe)
 * and from an unknown outcome (only a human can tell).
 */
public class SupplierOrderAwaitingSupplierException extends SupplierOrderException {

    private final String externalOrderId;
    private final List<String> pendingLines;

    public SupplierOrderAwaitingSupplierException(String externalOrderId, List<String> pendingLines, String message) {
        super(message);
        if (externalOrderId == null || externalOrderId.isBlank()) {
            throw new IllegalArgumentException("An order awaiting the supplier needs its external order id");
        }
        this.externalOrderId = externalOrderId;
        this.pendingLines = pendingLines == null ? List.of() : List.copyOf(pendingLines);
    }

    public String externalOrderId() {
        return externalOrderId;
    }

    /** Human-readable lines still unconfirmed, e.g. {@code "OBUASUOBU0061: 0 of 1 reserved"}. */
    public List<String> pendingLines() {
        return pendingLines;
    }
}
