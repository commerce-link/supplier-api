package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;
import pl.commercelink.taxonomy.ProductCategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxonomyTest {

    @Test
    void categoryIsAString() {
        // given / when
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro", "Laptops", 10, null, null);

        // then
        assertEquals("Laptops", taxonomy.category());
    }

    @Test
    void enumConstructorBridgesToCategoryName() {
        // given / when
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro",
                ProductCategory.Laptops, 10, 1200, 1500);

        // then
        assertEquals("Laptops", taxonomy.category());
        assertEquals(1200, taxonomy.netWeightInGrams());
        assertEquals(1500, taxonomy.grossWeightInGrams());
    }

    @Test
    void shortEnumConstructorBridgesToCategoryName() {
        // given / when
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro",
                ProductCategory.Laptops, 10);

        // then
        assertEquals("Laptops", taxonomy.category());
        assertNull(taxonomy.netWeightInGrams());
        assertNull(taxonomy.grossWeightInGrams());
    }

    @Test
    void nullEnumCategoryBridgesToNullString() {
        // given / when
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro",
                (ProductCategory) null, 10);

        // then
        assertNull(taxonomy.category());
    }

    @Test
    void enumConstructorUnifiesEanAndMfn() {
        // given / when
        Taxonomy taxonomy = new Taxonomy("0590123412345", "mfn 1", "BrandX", "Laptop Pro",
                ProductCategory.Laptops, 10);

        // then
        assertEquals("590123412345", taxonomy.ean());
        assertEquals("MFN1", taxonomy.mfn());
    }

    @Test
    void emptyTaxonomyHasOtherCategory() {
        // when / then
        assertEquals("Other", Taxonomy.EMPTY.category());
    }

    @Test
    void completeTaxonomyIsProcessable() {
        // given
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro", "Laptops", 10, null, null);

        // when / then
        assertTrue(taxonomy.isProcessable());
    }

    @Test
    void otherCategoryIsNotProcessable() {
        // given
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro", "Other", 10, null, null);

        // when / then
        assertFalse(taxonomy.isProcessable());
    }

    @Test
    void nullCategoryIsNotProcessable() {
        // given
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro", (String) null, 10, null, null);

        // when / then
        assertFalse(taxonomy.isProcessable());
    }
}
