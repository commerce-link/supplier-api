package pl.commercelink.inventory.supplier.api;

public sealed interface ShippingCostPolicy {
    double calculate(double totalSourceValue);

    record FlatRate(double threshold, double cost) implements ShippingCostPolicy {
        public double calculate(double totalSourceValue) {
            return totalSourceValue >= threshold ? 0 : cost;
        }
    }

    record Free() implements ShippingCostPolicy {
        public double calculate(double totalSourceValue) { return 0; }
    }
}
