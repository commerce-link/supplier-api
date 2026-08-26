package pl.commercelink.inventory.supplier.api;

/**
 * Carrier pickup point (parcel locker, PUDO) chosen by the end customer of a dropship order.
 * {@code carrier} is the application's canonical carrier name (e.g. {@code InPost}, {@code DPD});
 * {@code code} is the point code as the marketplace reports it. The address fields are optional —
 * the application cannot always fill them, so adapters must work from carrier and code alone.
 */
public record SupplierPickupPoint(String carrier, String code,
                                  String name, String streetAndNumber, String postalCode, String city) {

    public SupplierPickupPoint {
        carrier = required(carrier, "carrier");
        code = required(code, "code");
        name = trimToNull(name);
        streetAndNumber = trimToNull(streetAndNumber);
        postalCode = trimToNull(postalCode);
        city = trimToNull(city);
    }

    private static String required(String value, String what) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException("Pickup point " + what + " is required");
        }
        return trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
