package pl.commercelink.inventory.supplier.api;

import pl.commercelink.taxonomy.ProductCategory;
import pl.commercelink.taxonomy.UnifiedProductIdentifiers;

public record Taxonomy(String ean, String mfn, String brand, String name, ProductCategory category, int dataAccuracyScore) {

    public static final Taxonomy EMPTY = new Taxonomy("N/A", "N/A", "N/A", "N/A", ProductCategory.Other, Integer.MAX_VALUE);

    public Taxonomy {
        ean = UnifiedProductIdentifiers.unifyEan(ean);
        mfn = UnifiedProductIdentifiers.unifyMfn(mfn);
    }

    public boolean isProcessable() {
        return category != null
                && ean != null && !ean.isEmpty()
                && mfn != null && !mfn.isEmpty()
                && brand != null && !brand.isEmpty()
                && name != null && !name.isEmpty()
                && category != ProductCategory.Other;
    }
}
