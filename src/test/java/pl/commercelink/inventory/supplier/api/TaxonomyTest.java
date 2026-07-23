package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

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
    void constructorUnifiesEanAndMfn() {
        // given / when
        Taxonomy taxonomy = new Taxonomy("0590123412345", "mfn 1", "BrandX", "Laptop Pro", "Laptops", 10, null, null);

        // then
        assertEquals("590123412345", taxonomy.ean());
        assertEquals("MFN1", taxonomy.mfn());
    }

    @Test
    void emptyTaxonomyHasNullCategory() {
        // when / then
        assertNull(Taxonomy.EMPTY.category());
    }

    @Test
    void completeTaxonomyIsProcessable() {
        // given
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro", "Laptops", 10, null, null);

        // when / then
        assertTrue(taxonomy.isProcessable());
    }

    @Test
    void categorizedRowWithBlankIdentifierIsNotProcessable() {
        // given
        Taxonomy taxonomy = new Taxonomy("", "", "BrandX", "Laptop Pro", "Laptops", 10, null, null);

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

    @Test
    void eightArgConstructorLeavesRawCategoryNull() {
        // given
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro", "Laptops", 10, null, null);

        // when / then
        assertNull(taxonomy.rawCategory());
    }

    @Test
    void rawCategoryIsCarriedThroughCanonicalConstructor() {
        // given
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro", "Laptops", 10, null, null, "Elektronika > Laptopy");

        // when / then
        assertEquals("Elektronika > Laptopy", taxonomy.rawCategory());
    }

    @Test
    void nineArgConstructorLeavesCategoryIdNull() {
        // given
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro", "Laptops", 10, null, null, "Elektronika > Laptopy");

        // when / then
        assertNull(taxonomy.categoryId());
    }

    @Test
    void tenArgConstructorStoresCategoryId() {
        // given
        Taxonomy taxonomy = new Taxonomy("5901234123457", "MFN-1", "BrandX", "Laptop Pro", "Laptops", 10, null, null, "Elektronika > Laptopy", "195");

        // when / then
        assertEquals("195", taxonomy.categoryId());
    }
}
