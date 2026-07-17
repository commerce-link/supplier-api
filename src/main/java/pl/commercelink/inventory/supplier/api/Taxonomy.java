package pl.commercelink.inventory.supplier.api;

import pl.commercelink.taxonomy.UnifiedProductIdentifiers;

public record Taxonomy(String ean, String mfn, String brand, String name,
                       String category, int dataAccuracyScore,
                       Integer netWeightInGrams, Integer grossWeightInGrams) {

    public static final String OTHER = "Other";

    public static final Taxonomy EMPTY = new Taxonomy("N/A", "N/A", "N/A", "N/A",
            OTHER, Integer.MAX_VALUE, null, null);

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
                && !category.equals(OTHER);
    }
}
