package pl.commercelink.inventory.supplier.api;

/** One selectable value of a {@link SupplierOrderOption}; {@code hint} is optional UI help (e.g. "max 30 kg"). */
public record SupplierOrderOptionChoice(String value, String label, String hint) {
    public SupplierOrderOptionChoice {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Order option choice value is required");
        }
        if (label == null || label.isBlank()) {
            label = value;
        }
    }
}
