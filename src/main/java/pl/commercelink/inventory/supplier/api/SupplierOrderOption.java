package pl.commercelink.inventory.supplier.api;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A decision the supplier needs per order (e.g. delivery method), rendered by the application as a
 * select. {@code defaultValue} only preselects the UI; adapters never substitute it for a missing
 * choice. The chosen {@code value} travels back in {@code SupplierPurchaseRequest#options()} /
 * {@code SupplierDropshipRequest#options()} under {@code key}.
 */
public record SupplierOrderOption(String key, String label, List<SupplierOrderOptionChoice> choices,
                                  String defaultValue, boolean required) {
    public SupplierOrderOption {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Order option key is required");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Order option label is required");
        }
        choices = choices == null ? List.of() : List.copyOf(choices);
        Set<String> values = new HashSet<>();
        for (SupplierOrderOptionChoice choice : choices) {
            if (!values.add(choice.value())) {
                throw new IllegalArgumentException("Duplicate order option choice: " + choice.value());
            }
        }
        if (defaultValue != null && !values.contains(defaultValue)) {
            throw new IllegalArgumentException("Default value " + defaultValue + " is not among the choices of " + key);
        }
    }

    public Optional<SupplierOrderOptionChoice> choice(String value) {
        return choices.stream().filter(choice -> choice.value().equals(value)).findFirst();
    }
}
