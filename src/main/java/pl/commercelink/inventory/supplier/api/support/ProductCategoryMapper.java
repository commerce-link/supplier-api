package pl.commercelink.inventory.supplier.api.support;

import pl.commercelink.inventory.supplier.api.Taxonomy;

import java.util.Arrays;
import java.util.List;

public class ProductCategoryMapper {

    private List<Mapping> mappings;

    public ProductCategoryMapper(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    public String from(String group, String subgroup) {
        for (Mapping mapping : mappings) {
            if (mapping.matches(group, subgroup)) {
                return mapping.productCategory;
            }
        }
        return Taxonomy.OTHER;
    }

    public static class Mapping {
        private final String productCategory;
        private String group;
        private List<String> subgroups;

        public Mapping(String productCategory, String group, String[] subgroups) {
            this.productCategory = productCategory;

            this.group = group;
            this.subgroups = Arrays.asList(subgroups);
        }

        boolean matches(String group, String subgroup) {
            return this.group.equalsIgnoreCase(group) && this.subgroups.contains(subgroup);
        }

    }

}
