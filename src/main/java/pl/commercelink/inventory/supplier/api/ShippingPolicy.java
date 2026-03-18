package pl.commercelink.inventory.supplier.api;

import java.util.Map;

public record ShippingPolicy(
        ShippingTerms local,
        ShippingTerms foreign,
        Map<String, ShippingTerms> countryOverrides
) {

    public ShippingPolicy(ShippingTerms terms) {
        this(terms, terms, Map.of());
    }

    public ShippingPolicy(ShippingTerms local, ShippingTerms foreign) {
        this(local, foreign, Map.of());
    }
}
