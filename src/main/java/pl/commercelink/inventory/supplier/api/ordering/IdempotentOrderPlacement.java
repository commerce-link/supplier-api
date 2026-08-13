package pl.commercelink.inventory.supplier.api.ordering;

import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
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
            O order = wrapFailures("order placement failed",
                    () -> placeNewOrder(request, lines));
            String externalOrderId = wrapFailures("order id extraction failed",
                    () -> order == null ? null : externalOrderId(order));
            if (externalOrderId == null || externalOrderId.isBlank()) {
                throw new SupplierOrderException(
                        supplierName() + " returned no purchase order id for ref " + clientOrderRef);
            }
            return wrapFailures("order result mapping failed", () -> toResult(order, request));
        }
    }

    protected abstract String supplierName();

    protected abstract L toSupplierLine(SupplierOrderLine line);

    protected abstract Optional<O> findExistingOrder(String clientOrderRef);

    protected abstract O placeNewOrder(SupplierPurchaseRequest request, List<L> lines);

    protected abstract String externalOrderId(O order);

    protected abstract SupplierOrderResult toResult(O order, SupplierPurchaseRequest request);

    private <T> T wrapFailures(String activity, Supplier<T> action) {
        try {
            return action.get();
        } catch (SupplierOrderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SupplierOrderException(supplierName() + " " + activity, e);
        }
    }
}
