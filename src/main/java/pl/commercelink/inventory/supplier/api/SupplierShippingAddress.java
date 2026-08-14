package pl.commercelink.inventory.supplier.api;

/**
 * A literal recipient address sent inside the order document, for suppliers that accept a
 * free-form delivery address instead of a handle on one registered on the account
 * (see {@link SupplierDeliveryAddress} for that other model).
 */
public record SupplierShippingAddress(
        String company,
        String firstName,
        String lastName,
        String streetAndNumber,
        String postalCode,
        String city,
        String country,
        String phone,
        String email
) {

    public SupplierShippingAddress {
        requireFilled(streetAndNumber, "street and number");
        requireFilled(postalCode, "postal code");
        requireFilled(city, "city");
        requireFilled(country, "country");
        if (isBlank(company) && (isBlank(firstName) || isBlank(lastName))) {
            throw new IllegalArgumentException("Shipping address needs a company or a first and last name");
        }
        country = country.trim().toUpperCase();
        if (country.length() != 2) {
            throw new IllegalArgumentException("Country must be an ISO 3166 alpha-2 code: " + country);
        }
    }

    private static void requireFilled(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("Shipping address " + field + " is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
