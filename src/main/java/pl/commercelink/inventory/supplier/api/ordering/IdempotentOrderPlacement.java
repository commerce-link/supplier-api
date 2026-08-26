package pl.commercelink.inventory.supplier.api.ordering;

import pl.commercelink.inventory.supplier.api.SupplierDropshipRequest;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderOutcomeUnknownException;
import pl.commercelink.inventory.supplier.api.SupplierOrderRejectedException;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierProvider;
import pl.commercelink.inventory.supplier.api.SupplierPurchaseRequest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class IdempotentOrderPlacement<L, O> {

    private static final ConcurrentHashMap<String, Object> ORDER_LOCKS = new ConcurrentHashMap<>();

    protected final SupplierOrderResult placeIdempotently(SupplierPurchaseRequest request) {
        String clientOrderRef = request.clientOrderRef();
        if (clientOrderRef == null || clientOrderRef.isBlank()) {
            throw new SupplierOrderException(
                    "Missing clientOrderRef, refusing to place a non-idempotent " + supplierName() + " order");
        }
        List<L> lines = wrapFailures("order line translation failed",
                () -> request.lines().stream().map(this::toSupplierLine).toList());
        synchronized (ORDER_LOCKS.computeIfAbsent(getClass().getName() + "|" + clientOrderRef, key -> new Object())) {
            Optional<O> existing = wrapFailures("replay check failed",
                    () -> findExistingOrder(clientOrderRef));
            if (existing.isPresent()) {
                return wrapFailures("order result mapping failed",
                        () -> toResult(existing.orElseThrow(), request));
            }
            O order = wrapPlacementFailures("order placement failed",
                    () -> placeNewOrder(request, lines));
            String externalOrderId = wrapPlacementFailures("order id extraction failed",
                    () -> order == null ? null : externalOrderId(order));
            if (externalOrderId == null || externalOrderId.isBlank()) {
                throw new SupplierOrderOutcomeUnknownException(
                        supplierName() + " returned no purchase order id for ref " + clientOrderRef);
            }
            return wrapPlacementFailures("order result mapping failed", () -> toResult(order, request));
        }
    }

    protected final SupplierOrderResult placeDropshipIdempotently(SupplierDropshipRequest request) {
        String clientOrderRef = request.clientOrderRef();
        if (clientOrderRef == null || clientOrderRef.isBlank()) {
            throw new SupplierOrderException(
                    "Missing clientOrderRef, refusing to place a non-idempotent " + supplierName() + " dropship order");
        }
        if (request.consignee() == null) {
            throw new SupplierOrderException(
                    "Missing consignee, refusing to place a " + supplierName() + " dropship order");
        }
        if (request.pickupPoint() != null && !acceptsPickupPoint()) {
            // Never redirect a parcel the customer wants at a pickup point to the street address.
            throw new SupplierOrderRejectedException(supplierName()
                    + " does not deliver dropship orders to carrier pickup points (requested "
                    + request.pickupPoint().carrier() + " " + request.pickupPoint().code() + ")");
        }
        List<L> lines = wrapFailures("dropship line translation failed",
                () -> request.lines().stream().map(this::toSupplierLine).toList());
        // Separate |DS| namespace: a dropship retry must replay the dropship order, never contend
        // with a regular purchase that happens to reuse the same ref.
        synchronized (ORDER_LOCKS.computeIfAbsent(getClass().getName() + "|DS|" + clientOrderRef, key -> new Object())) {
            Optional<O> existing = wrapFailures("dropship replay check failed",
                    () -> findExistingDropshipOrder(clientOrderRef));
            if (existing.isPresent()) {
                return wrapFailures("dropship result mapping failed",
                        () -> toDropshipResult(existing.orElseThrow(), request));
            }
            O order = wrapPlacementFailures("dropship order placement failed",
                    () -> placeNewDropshipOrder(request, lines));
            String externalOrderId = wrapPlacementFailures("dropship order id extraction failed",
                    () -> order == null ? null : externalOrderId(order));
            if (externalOrderId == null || externalOrderId.isBlank()) {
                throw new SupplierOrderOutcomeUnknownException(
                        supplierName() + " returned no dropship order id for ref " + clientOrderRef);
            }
            return wrapPlacementFailures("dropship result mapping failed", () -> toDropshipResult(order, request));
        }
    }

    /**
     * Read-only probe: the order previously placed with {@code request.clientOrderRef()},
     * if this supplier reports one. MUST never place a new order.
     */
    public Optional<SupplierOrderResult> findPlacedOrder(SupplierPurchaseRequest request) {
        String clientOrderRef = request.clientOrderRef();
        if (clientOrderRef == null || clientOrderRef.isBlank()) {
            return Optional.empty();
        }
        // Dropship orders may live in a different supplier view (e.g. Incom's DR list), so the
        // probe checks both before reporting "not found".
        return wrapFailures("placed order lookup failed",
                () -> findExistingOrder(clientOrderRef)
                        .or(() -> findExistingDropshipOrder(clientOrderRef))
                        .map(order -> toResult(order, request)));
    }

    protected abstract String supplierName();

    protected abstract L toSupplierLine(SupplierOrderLine line);

    protected abstract Optional<O> findExistingOrder(String clientOrderRef);

    protected abstract O placeNewOrder(SupplierPurchaseRequest request, List<L> lines);

    protected abstract String externalOrderId(O order);

    protected abstract SupplierOrderResult toResult(O order, SupplierPurchaseRequest request);

    protected Optional<O> findExistingDropshipOrder(String clientOrderRef) {
        return findExistingOrder(clientOrderRef);
    }

    /** Whether {@link #placeNewDropshipOrder} honours a pickup point; providers answer via the SPI flag. */
    protected boolean acceptsPickupPoint() {
        return this instanceof SupplierProvider provider && provider.supportsPickupPointDropship();
    }

    protected O placeNewDropshipOrder(SupplierDropshipRequest request, List<L> lines) {
        throw new UnsupportedOperationException(supplierName() + " does not support dropship placement");
    }

    protected SupplierOrderResult toDropshipResult(O order, SupplierDropshipRequest request) {
        return toResult(order, new SupplierPurchaseRequest(request.clientOrderRef(), request.lines()));
    }

    private <T> T wrapFailures(String activity, Supplier<T> action) {
        try {
            return action.get();
        } catch (SupplierOrderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SupplierOrderException(supplierName() + " " + activity, e);
        }
    }

    private <T> T wrapPlacementFailures(String activity, Supplier<T> action) {
        try {
            return action.get();
        } catch (SupplierOrderRejectedException | SupplierOrderOutcomeUnknownException e) {
            throw e;
        } catch (SupplierOrderException e) {
            // The request may have left the process: without an explicit rejection the
            // supplier might have registered the order, so the outcome is unknown.
            throw new SupplierOrderOutcomeUnknownException(
                    supplierName() + " " + activity + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new SupplierOrderOutcomeUnknownException(supplierName() + " " + activity, e);
        }
    }
}
