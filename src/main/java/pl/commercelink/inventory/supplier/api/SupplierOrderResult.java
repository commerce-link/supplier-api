package pl.commercelink.inventory.supplier.api;

import java.util.List;

public record SupplierOrderResult(String externalOrderId, double totalNet, String currency,
                                  List<SupplierQuote> confirmedLines) {}
