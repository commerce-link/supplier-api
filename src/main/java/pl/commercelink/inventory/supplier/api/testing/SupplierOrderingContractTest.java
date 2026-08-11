package pl.commercelink.inventory.supplier.api.testing;

import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierProvider;
import pl.commercelink.inventory.supplier.api.SupplierPurchaseRequest;
import pl.commercelink.inventory.supplier.api.SupplierQuote;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Executable ordering contract for {@link SupplierProvider} implementations; every ordering
 * adapter ships a test class extending this kit. Required hooks build providers backed by the
 * adapter's own fixtures; optional hooks cover failure scenarios not every adapter can simulate
 * and skip their tests when left at the defaults. The enforced rules are documented in the
 * supplier-api README under "Ordering contract".
 */
public abstract class SupplierOrderingContractTest {

    /** Provider able to fulfil every line of {@link #sampleLines()}. */
    protected abstract SupplierProvider providerFullyAvailable();

    /** Provider short on at least one line of {@link #sampleLines()}. */
    protected abstract SupplierProvider providerWithShortage();

    /** Order lines known to the adapter's fixture, each with a filled supplier sku and fully orderable from {@link #providerFullyAvailable()}. */
    protected abstract List<SupplierOrderLine> sampleLines();

    /** A client order reference never used before in this JVM (adapters may cache orders statically). */
    protected abstract String uniqueClientOrderRef();

    /** Provider whose backend answers a placement with a blank or missing order id. */
    protected Optional<SupplierProvider> providerReturningBlankOrderId() {
        return Optional.empty();
    }

    /** Provider whose backend fails every remote call with a raw transport error. */
    protected Optional<SupplierProvider> providerWithFailingBackend() {
        return Optional.empty();
    }

    /** Provider whose backend knows the sample products but reports no usable price. */
    protected Optional<SupplierProvider> providerWithMissingPrice() {
        return Optional.empty();
    }

    /** A line for a product the supplier does not carry — its {@code sku} is null. */
    protected SupplierOrderLine unknownProductLine() {
        return new SupplierOrderLine(null, "9999999999990", "TCK-UNKNOWN", 1);
    }

    /** Number of orders actually placed at the (fake) supplier since the test started, if observable. */
    protected OptionalInt remoteOrdersPlaced() {
        return OptionalInt.empty();
    }

    /** Number of remote backend interactions since the test started, if observable. */
    protected OptionalInt remoteCalls() {
        return OptionalInt.empty();
    }

    @Test
    void providerReportsOrderingSupport() {
        // when / then
        assertTrue(providerFullyAvailable().supportsOrdering());
    }

    @Test
    void placeOrderReturnsExternalOrderId() {
        // given
        SupplierProvider provider = providerFullyAvailable();

        // when
        SupplierOrderResult result = provider.placeOrder(
                new SupplierPurchaseRequest(uniqueClientOrderRef(), sampleLines()));

        // then
        assertNotNull(result.externalOrderId());
        assertFalse(result.externalOrderId().isBlank());
    }

    @Test
    void placeOrderRetriedWithSameRefDoesNotPlaceSecondOrder() {
        // given
        SupplierProvider provider = providerFullyAvailable();
        SupplierPurchaseRequest request = new SupplierPurchaseRequest(uniqueClientOrderRef(), sampleLines());

        // when
        SupplierOrderResult first = provider.placeOrder(request);
        SupplierOrderResult second = provider.placeOrder(request);

        // then
        assertEquals(first.externalOrderId(), second.externalOrderId());
        assertEquals(first.totalNet(), second.totalNet());
        remoteOrdersPlaced().ifPresent(count -> assertEquals(1, count));
    }

    @Test
    void placeOrderWithShortageThrowsWithoutPlacingPartialOrder() {
        // given
        SupplierProvider provider = providerWithShortage();
        SupplierPurchaseRequest request = new SupplierPurchaseRequest(uniqueClientOrderRef(), sampleLines());

        // when / then
        assertThrows(SupplierOrderException.class, () -> provider.placeOrder(request));
        remoteOrdersPlaced().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void placeOrderWithBlankRefThrowsBeforeAnyRemoteCall() {
        // given
        SupplierProvider provider = providerFullyAvailable();

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> provider.placeOrder(new SupplierPurchaseRequest(" ", sampleLines())));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void placeOrderWithNullRefThrowsBeforeAnyRemoteCall() {
        // given
        SupplierProvider provider = providerFullyAvailable();

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> provider.placeOrder(new SupplierPurchaseRequest(null, sampleLines())));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void placeOrderWithBlankExternalOrderIdThrows() {
        // given
        SupplierProvider provider = assumePresent(providerReturningBlankOrderId(), "a blank order id response");

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> provider.placeOrder(new SupplierPurchaseRequest(uniqueClientOrderRef(), sampleLines())));
    }

    @Test
    void placeOrderFailureSurfacesOnlyAsSupplierOrderException() {
        // given
        SupplierProvider provider = assumePresent(providerWithFailingBackend(), "a failing backend");

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> provider.placeOrder(new SupplierPurchaseRequest(uniqueClientOrderRef(), sampleLines())));
    }

    @Test
    void checkAvailabilityFailureSurfacesOnlyAsSupplierOrderException() {
        // given
        SupplierProvider provider = assumePresent(providerWithFailingBackend(), "a failing backend");

        // when / then
        assertThrows(SupplierOrderException.class, () -> provider.checkAvailability(sampleLines()));
    }

    @Test
    void checkAvailabilityQuotesZeroQuantityForUnknownProduct() {
        // given
        SupplierProvider provider = providerFullyAvailable();

        // when
        List<SupplierQuote> quotes = provider.checkAvailability(List.of(unknownProductLine()));

        // then
        assertEquals(1, quotes.size());
        assertEquals(0, quotes.getFirst().availableQuantity());
    }

    @Test
    void checkAvailabilityQuotesZeroQuantityWhenPriceMissing() {
        // given
        SupplierProvider provider = assumePresent(providerWithMissingPrice(), "a missing price response");

        // when
        List<SupplierQuote> quotes = provider.checkAvailability(sampleLines());

        // then
        assertFalse(quotes.isEmpty());
        quotes.forEach(quote -> assertEquals(0, quote.availableQuantity()));
    }

    private static SupplierProvider assumePresent(Optional<SupplierProvider> provider, String scenario) {
        assumeTrue(provider.isPresent(), "Adapter does not simulate " + scenario + " — hook not implemented");
        return provider.orElseThrow();
    }
}
