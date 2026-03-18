package pl.commercelink.inventory.supplier.api;

import java.util.Optional;

import static pl.commercelink.inventory.supplier.api.UnifiedProductIdentifiers.unifyEan;
import static pl.commercelink.inventory.supplier.api.UnifiedProductIdentifiers.unifyMfn;

public record InventoryItem(
        String ean,
        String mfn,
        double netPrice,
        String currency,
        int qty,
        int leadTimeDays,
        String supplier,
        boolean sellable,
        boolean inStock,
        boolean inDelivery
) {

    private static final String MISSING_EAN_PLACEHOLDER = "1111111111111";

    public InventoryItem {
        ean = unifyEan(ean);
        mfn = unifyMfn(mfn);
    }

    public InventoryItem(String ean, String mfn, double netPrice, String currency, int qty, int leadTimeDays, String supplier) {
        this(ean, mfn, netPrice, currency, qty, leadTimeDays, supplier, true, false, false);
    }

    public InventoryItem(String ean, String mfn, double netPrice, String currency, int qty, int leadTimeDays, String supplier, boolean sellable) {
        this(ean, mfn, netPrice, currency, qty, leadTimeDays, supplier, sellable, false, false);
    }

    public String uuid() {
        return supplier + "_" + ean + "_" + mfn;
    }

    public boolean hasQty() {
        return qty > 0;
    }

    public boolean hasEan() {
        return ean != null && !ean.isBlank() && !MISSING_EAN_PLACEHOLDER.equalsIgnoreCase(ean);
    }

    public boolean hasMfn() {
        return mfn != null && !mfn.isBlank() && !"0".equalsIgnoreCase(mfn);
    }

    public boolean hasSupplier(String other) {
        return supplier.equals(other);
    }

    public boolean isSellable() {
        return hasEan() && hasMfn() && hasQty() && sellable;
    }

    public Optional<InventoryItem> toLocalCurrency(String localCurrency, Double exchangeRate) {
        if (localCurrency.equals(currency)) return Optional.of(this);
        if (exchangeRate == null) return Optional.empty();
        return Optional.of(new InventoryItem(ean, mfn, Math.round(netPrice * exchangeRate), localCurrency, qty, leadTimeDays, supplier, sellable, inStock, inDelivery));
    }

    public InventoryItem withEan(String newEan) {
        return new InventoryItem(newEan, mfn, netPrice, currency, qty, leadTimeDays, supplier, sellable, inStock, inDelivery);
    }

}
