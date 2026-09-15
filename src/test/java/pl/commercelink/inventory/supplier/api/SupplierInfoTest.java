package pl.commercelink.inventory.supplier.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SupplierInfoTest {

    private static final SupplierInfo INFO = new SupplierInfo("Kosatec", SupplierType.Distributor, 7, "PL",
            new ShippingPolicy(new ShippingTerms(1, new ShippingCostPolicy.Free())), "https://partner/{id}");

    @Test
    void withNameCarriesEveryOtherFieldOver() {
        // when
        SupplierInfo renamed = INFO.withName("Kosatec-k7f3a9c2");

        // then
        assertEquals("Kosatec-k7f3a9c2", renamed.name());
        assertEquals(INFO.type(), renamed.type());
        assertEquals(INFO.accuracyScore(), renamed.accuracyScore());
        assertEquals(INFO.origin(), renamed.origin());
        assertEquals(INFO.shippingPolicy(), renamed.shippingPolicy());
        assertEquals(INFO.partnerSiteUrlTemplate(), renamed.partnerSiteUrlTemplate());
    }

    @Test
    void withNameReturnsTheSameInstanceForTheSameName() {
        // when / then
        assertSame(INFO, INFO.withName("Kosatec"));
    }
}
