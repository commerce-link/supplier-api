package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SupplierPurchaseRequestTest {

    private static final List<SupplierOrderLine> LINES =
            List.of(new SupplierOrderLine("SKU-1", "5901234567890", "MFN-1", 1));

    @Test
    void twoArgConstructorLeavesBothAddressFieldsEmpty() {
        SupplierPurchaseRequest request = new SupplierPurchaseRequest("ref-1", LINES);
        assertNull(request.deliveryAddressId());
        assertNull(request.shippingAddress());
    }

    @Test
    void threeArgConstructorCarriesDeliveryAddressIdOnly() {
        SupplierPurchaseRequest request = new SupplierPurchaseRequest("ref-1", LINES, "42");
        assertEquals("42", request.deliveryAddressId());
        assertNull(request.shippingAddress());
    }

    @Test
    void shippingAddressIsNotAcceptedByDefault() {
        SupplierProvider provider = () -> java.util.Optional.empty();
        assertFalse(provider.acceptsShippingAddress());
    }
}
