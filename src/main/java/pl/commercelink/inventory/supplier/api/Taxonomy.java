package pl.commercelink.inventory.supplier.api;

import pl.commercelink.taxonomy.ProductCategory;
import pl.commercelink.taxonomy.UnifiedProductIdentifiers;

public record Taxonomy(String ean, String mfn, String brand, String name,
                       ProductCategory category, int dataAccuracyScore,
                       Integer netWeightInGrams, Integer grossWeightInGrams,
                       String categoryKey) {

    public static final Taxonomy EMPTY = new Taxonomy("N/A", "N/A", "N/A", "N/A",
            ProductCategory.Other, Integer.MAX_VALUE, null, null);

    public Taxonomy {
        ean = UnifiedProductIdentifiers.unifyEan(ean);
        mfn = UnifiedProductIdentifiers.unifyMfn(mfn);
        categoryKey = (categoryKey != null && !categoryKey.isBlank())
                ? categoryKey
                : (category != null ? category.name() : "Other");
    }

    public Taxonomy(String ean, String mfn, String brand, String name,
                    ProductCategory category, int dataAccuracyScore,
                    Integer netWeightInGrams, Integer grossWeightInGrams) {
        this(ean, mfn, brand, name, category, dataAccuracyScore, netWeightInGrams, grossWeightInGrams, null);
    }

    public Taxonomy(String ean, String mfn, String brand, String name,
                    ProductCategory category, int dataAccuracyScore) {
        this(ean, mfn, brand, name, category, dataAccuracyScore, null, null);
    }

    public boolean isProcessable() {
        return categoryKey != null && !"Other".equals(categoryKey)
                && ean != null && !ean.isEmpty()
                && mfn != null && !mfn.isEmpty()
                && brand != null && !brand.isEmpty()
                && name != null && !name.isEmpty();
    }
}
