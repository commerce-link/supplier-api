package pl.commercelink.inventory.supplier.api;

import pl.commercelink.taxonomy.UnifiedProductIdentifiers;

public record Taxonomy(String ean, String mfn, String brand, String name,
                       String category, int dataAccuracyScore,
                       Integer netWeightInGrams, Integer grossWeightInGrams,
                       String rawCategory, String categoryId) {

    public static final Taxonomy EMPTY = new Taxonomy("N/A", "N/A", "N/A", "N/A",
            null, Integer.MAX_VALUE, null, null);

    public Taxonomy(String ean, String mfn, String brand, String name, String category,
                    int dataAccuracyScore, Integer netWeightInGrams, Integer grossWeightInGrams,
                    String rawCategory) {
        this(ean, mfn, brand, name, category, dataAccuracyScore,
                netWeightInGrams, grossWeightInGrams, rawCategory, null);
    }

    public Taxonomy(String ean, String mfn, String brand, String name, String category,
                    int dataAccuracyScore, Integer netWeightInGrams, Integer grossWeightInGrams) {
        this(ean, mfn, brand, name, category, dataAccuracyScore,
                netWeightInGrams, grossWeightInGrams, null, null);
    }

    public Taxonomy {
        ean = UnifiedProductIdentifiers.unifyEan(ean);
        mfn = UnifiedProductIdentifiers.unifyMfn(mfn);
    }

    public boolean isProcessable() {
        return category != null && !category.isBlank()
                && ean != null && !ean.isEmpty()
                && mfn != null && !mfn.isEmpty()
                && brand != null && !brand.isEmpty()
                && name != null && !name.isEmpty();
    }
}
