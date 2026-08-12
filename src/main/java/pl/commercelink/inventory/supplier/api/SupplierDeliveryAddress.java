package pl.commercelink.inventory.supplier.api;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A delivery address registered on the store's account at the supplier. The {@code id} is the
 * value the supplier expects back in {@link SupplierPurchaseRequest#deliveryAddressId()}.
 */
public record SupplierDeliveryAddress(
        String id,
        String addressLine,
        String city,
        String postalCode,
        String country
) {

    public SupplierDeliveryAddress {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Delivery address id is required");
        }
    }

    public String label() {
        String label = Stream.of(addressLine, join(postalCode, city), country)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(", "));
        return label.isBlank() ? id : label;
    }

    private static String join(String postalCode, String city) {
        return Stream.of(postalCode, city)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }
}
