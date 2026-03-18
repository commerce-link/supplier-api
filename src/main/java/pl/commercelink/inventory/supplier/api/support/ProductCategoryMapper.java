package pl.commercelink.inventory.supplier.api.support;

import pl.commercelink.taxonomy.ProductCategory;

import java.util.Arrays;
import java.util.List;

public class ProductCategoryMapper {

    private List<Mapping> mappings;

    public ProductCategoryMapper(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    public ProductCategory from(String group, String subgroup) {
        for (Mapping mapping : mappings) {
            if (mapping.matches(group, subgroup)) {
                return mapping.productCategory;
            }
        }
        return ProductCategory.Other;
    }

    public static class Mapping {
        private final ProductCategory productCategory;
        private String group;
        private List<String> subgroups;

        public Mapping(ProductCategory productCategory, String group, String[] subgroups) {
            this.productCategory = productCategory;

            this.group = group;
            this.subgroups = Arrays.asList(subgroups);
        }

        boolean matches(String group, String subgroup) {
            return this.group.equalsIgnoreCase(group) && this.subgroups.contains(subgroup);
        }

    }

}
