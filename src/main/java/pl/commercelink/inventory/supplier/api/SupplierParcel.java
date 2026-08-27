package pl.commercelink.inventory.supplier.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One parcel the supplier handed to a carrier. {@code carrier} and {@code trackingNo} are
 * mandatory because the application cannot confirm a shipment without them; {@code trackingUrl}
 * and {@code shippedAt} are optional. {@code lines} lists what is inside when the supplier
 * reports the split; an empty list means "split unknown" and the application treats the parcel
 * as carrying everything still outstanding.
 */
public record SupplierParcel(String carrier, String trackingNo, String trackingUrl,
                             LocalDateTime shippedAt, List<SupplierOrderLine> lines) {

    public SupplierParcel {
        carrier = required(carrier, "carrier");
        trackingNo = required(trackingNo, "tracking number");
        trackingUrl = trackingUrl == null || trackingUrl.isBlank() ? null : trackingUrl.trim();
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    private static String required(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parcel " + what + " is required");
        }
        return value.trim();
    }
}
