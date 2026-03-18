package pl.commercelink.inventory.supplier.api;

public record SupplierInfo(
        String name,
        SupplierType type,
        int accuracyScore,
        String origin,
        ShippingPolicy shippingPolicy,
        String partnerSiteUrlTemplate
) {

    public SupplierInfo(String name, SupplierType type, int accuracyScore, String origin,
                        ShippingPolicy shippingPolicy) {
        this(name, type, accuracyScore, origin, shippingPolicy, null);
    }

    public boolean isLocalFor(String destination) {
        return origin.equalsIgnoreCase(destination);
    }

    public ShippingTerms shippingTermsFor(String destination) {
        if (isLocalFor(destination)) {
            return shippingPolicy.local();
        }
        var override = shippingPolicy.countryOverrides().get(destination.toUpperCase());
        return override != null ? override : shippingPolicy.foreign();
    }

    public String getPartnerSiteUrl(String externalDeliveryId) {
        if (partnerSiteUrlTemplate == null) {
            return null;
        }
        return String.format(partnerSiteUrlTemplate, externalDeliveryId);
    }
}
