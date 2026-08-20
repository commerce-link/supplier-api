package pl.commercelink.inventory.supplier.api;

import java.util.Locale;
import java.util.Set;

/**
 * The end customer receiving a dropship order directly from the supplier. Stricter than a
 * warehouse address on purpose: carriers notify the recipient themselves, so {@code phone} and
 * {@code email} are mandatory (e.g. ELKO B2C requires both on every EndUser order). Either
 * {@code company} or both personal names identify the recipient; {@code country} is ISO-3166
 * alpha-2 because that is what supplier order APIs accept.
 */
public record SupplierConsignee(
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

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public SupplierConsignee {
        require(streetAndNumber, "street and number");
        require(postalCode, "postal code");
        require(city, "city");
        require(country, "country");
        require(phone, "phone");
        require(email, "email");
        if (isBlank(company) && (isBlank(firstName) || isBlank(lastName))) {
            throw new IllegalArgumentException(
                    "Consignee needs a company or a full personal name");
        }
        if (!ISO_COUNTRIES.contains(country)) {
            throw new IllegalArgumentException(
                    "Consignee country must be an ISO-3166 alpha-2 code, got: " + country);
        }
    }

    public String recipientName() {
        return isBlank(company) ? firstName + " " + lastName : company;
    }

    private static void require(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("Consignee " + field + " is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
