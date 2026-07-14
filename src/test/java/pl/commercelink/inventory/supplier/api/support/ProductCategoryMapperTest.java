package pl.commercelink.inventory.supplier.api.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductCategoryMapperTest {

    private final ProductCategoryMapper mapper = new ProductCategoryMapper(List.of(
            new ProductCategoryMapper.Mapping("GPU", "Components", new String[]{"Graphics Cards"}),
            new ProductCategoryMapper.Mapping("CPU", "Components", new String[]{"Processors", "APU"}),
            new ProductCategoryMapper.Mapping("Laptops", "Computers", new String[]{"Notebooks"})));

    @Test
    void mapsGroupAndSubgroupToCategory() {
        // when / then
        assertEquals("GPU", mapper.from("Components", "Graphics Cards").toString());
        assertEquals("CPU", mapper.from("Components", "Processors").toString());
        assertEquals("CPU", mapper.from("Components", "APU").toString());
        assertEquals("Laptops", mapper.from("Computers", "Notebooks").toString());
    }

    @Test
    void matchesGroupCaseInsensitivelyAndSubgroupExactly() {
        // when / then
        assertEquals("GPU", mapper.from("COMPONENTS", "Graphics Cards").toString());
        assertEquals("Other", mapper.from("Components", "graphics cards").toString());
    }

    @Test
    void fallsBackToOtherForUnknownGroupOrSubgroup() {
        // when / then
        assertEquals("Other", mapper.from("Furniture", "Chairs").toString());
        assertEquals("Other", mapper.from("Components", "Chairs").toString());
    }
}
