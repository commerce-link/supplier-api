package pl.commercelink.inventory.supplier.api.testing;

import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.SupplierDeliveryAddress;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderOption;
import pl.commercelink.inventory.supplier.api.SupplierOrderOptionsContext;
import pl.commercelink.inventory.supplier.api.SupplierOrderOutcomeUnknownException;
import pl.commercelink.inventory.supplier.api.SupplierOrderRejectedException;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierProvider;
import pl.commercelink.inventory.supplier.api.SupplierPurchaseRequest;
import pl.commercelink.inventory.supplier.api.SupplierQuote;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Executable ordering contract for {@link SupplierProvider} implementations; every ordering
 * adapter ships a test class extending this kit. Required hooks build providers backed by the
 * adapter's own fixtures; optional hooks cover failure scenarios not every adapter can simulate
 * and skip their tests when left at the defaults. The rules shared with the dropship contract
 * (idempotency, all-or-nothing, ref guard, single failure type) are enforced by the common base
 * {@link SupplierPlacementContractTest}. The enforced rules are documented in the supplier-api
 * README under "Ordering contract".
 */
public abstract class SupplierOrderingContractTest extends SupplierPlacementContractTest {

    /** Provider able to fulfil every line of {@link #sampleLines()}. */
    protected abstract SupplierProvider providerFullyAvailable();

    /** Provider short on at least one line of {@link #sampleLines()}. */
    protected abstract SupplierProvider providerWithShortage();

    /**
     * Provider whose transport fails while the placement request is in flight, after availability
     * has already passed — the order may or may not have reached the supplier.
     */
    protected abstract SupplierProvider providerWithPlacementTransportFailure();

    /** Provider whose supplier explicitly rejects the order (e.g. validation failure at the supplier). */
    protected abstract SupplierProvider providerRejectingOrders();

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

    /**
     * Delivery address the adapter's fixture accepts, for suppliers that require one; left null
     * by adapters whose supplier takes the address from the account.
     */
    protected String deliveryAddressId() {
        return null;
    }

    /** A line for a product the supplier does not carry — its {@code sku} is null. */
    protected SupplierOrderLine unknownProductLine() {
        return new SupplierOrderLine(null, "9999999999990", "TCK-UNKNOWN", 1);
    }

    /** Number of orders actually placed at the (fake) supplier since the test started, if observable. */
    protected OptionalInt remoteOrdersPlaced() {
        return OptionalInt.empty();
    }

    @Override
    protected final SupplierProvider placementProvider() {
        return providerFullyAvailable();
    }

    @Override
    protected final SupplierProvider placementProviderWithShortage() {
        return providerWithShortage();
    }

    @Override
    protected final Optional<SupplierProvider> placementProviderWithFailingBackend() {
        return providerWithFailingBackend();
    }

    @Override
    protected final OptionalInt remotePlacedOrders() {
        return remoteOrdersPlaced();
    }

    @Override
    protected final boolean supportsPlacement(SupplierProvider provider) {
        return provider.supportsOrdering();
    }

    @Override
    protected final SupplierOrderResult place(SupplierProvider provider, String clientOrderRef,
                                              List<SupplierOrderLine> lines) {
        return provider.placeOrder(purchaseRequest(provider, clientOrderRef, lines));
    }

    @Override
    protected final SupplierOrderResult place(SupplierProvider provider, String clientOrderRef,
                                              List<SupplierOrderLine> lines, Map<String, String> options) {
        return provider.placeOrder(new SupplierPurchaseRequest(clientOrderRef, lines, deliveryAddressId(), options));
    }

    @Test
    void placeOrderWithBlankExternalOrderIdThrows() {
        // given
        SupplierProvider provider = assumePresent(providerReturningBlankOrderId(), "a blank order id response");

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> provider.placeOrder(purchaseRequest(provider, uniqueClientOrderRef(), sampleLines())));
    }

    @Test
    void placementTransportFailureSurfacesAsOutcomeUnknown() {
        // given
        SupplierProvider provider = providerWithPlacementTransportFailure();

        // when / then
        assertThrows(SupplierOrderOutcomeUnknownException.class,
                () -> provider.placeOrder(purchaseRequest(provider, uniqueClientOrderRef(), sampleLines())));
    }

    @Test
    void explicitRejectionSurfacesAsRejected() {
        // given
        SupplierProvider provider = providerRejectingOrders();

        // when / then
        assertThrows(SupplierOrderRejectedException.class,
                () -> provider.placeOrder(purchaseRequest(provider, uniqueClientOrderRef(), sampleLines())));
    }

    @Test
    void findPlacedOrderSeesPreviouslyPlacedOrder() {
        // given
        SupplierProvider provider = providerFullyAvailable();
        SupplierPurchaseRequest request = purchaseRequest(provider, uniqueClientOrderRef(), sampleLines());

        // when
        SupplierOrderResult placed = provider.placeOrder(request);
        Optional<SupplierOrderResult> found = provider.findPlacedOrder(request);

        // then
        assertTrue(found.isPresent());
        assertEquals(placed.externalOrderId(), found.get().externalOrderId());
    }

    @Test
    void findPlacedOrderReturnsEmptyForUnknownRef() {
        // given
        SupplierProvider provider = providerFullyAvailable();
        SupplierPurchaseRequest request = purchaseRequest(provider, uniqueClientOrderRef(), sampleLines());

        // when
        Optional<SupplierOrderResult> found = provider.findPlacedOrder(request);

        // then
        assertTrue(found.isEmpty());
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

    @Test
    void deliveryAddressesAreListedWhenRequired() {
        // given
        SupplierProvider provider = assumeAddressRequired();

        // when
        List<SupplierDeliveryAddress> addresses = provider.deliveryAddresses();

        // then
        assertFalse(addresses.isEmpty());
        addresses.forEach(address -> assertFalse(address.id().isBlank()));
    }

    @Test
    void placeOrderWithoutRequiredDeliveryAddressThrows() {
        // given
        SupplierProvider provider = assumeAddressRequired();

        // when / then
        assertThrows(SupplierOrderException.class, () -> provider.placeOrder(
                new SupplierPurchaseRequest(uniqueClientOrderRef(), sampleLines(), null)));
        remoteOrdersPlaced().ifPresent(count -> assertEquals(0, count));
    }

    private SupplierProvider assumeAddressRequired() {
        SupplierProvider provider = providerFullyAvailable();
        assumeTrue(provider.requiresDeliveryAddress(), "Supplier takes the delivery address from the account");
        return provider;
    }

    protected final SupplierPurchaseRequest purchaseRequest(SupplierProvider provider, String clientOrderRef,
                                                            List<SupplierOrderLine> lines) {
        return new SupplierPurchaseRequest(clientOrderRef, lines, deliveryAddressId(),
                sampleOptions(provider, SupplierOrderOptionsContext.warehouse()));
    }

    @Test
    void orderOptionsAreWellFormed() {
        List<SupplierOrderOption> options = assumeOrderOptions();

        assertEquals(options.size(), options.stream().map(SupplierOrderOption::key).distinct().count());
        options.forEach(option -> assertFalse(option.choices().isEmpty(), option.key() + " has no choices"));
    }

    @Test
    void placeOrderWithoutRequiredOptionThrowsRejected() {
        List<SupplierOrderOption> options = assumeOrderOptions();
        SupplierOrderOption required = options.stream().filter(SupplierOrderOption::required).findFirst()
                .orElseGet(() -> { assumeTrue(false, "No required option declared"); return null; });
        SupplierProvider provider = providerFullyAvailable();
        Map<String, String> chosen = new LinkedHashMap<>(sampleOptions(provider, SupplierOrderOptionsContext.warehouse()));
        chosen.remove(required.key());

        assertThrows(SupplierOrderRejectedException.class, () -> provider.placeOrder(
                new SupplierPurchaseRequest(uniqueClientOrderRef(), sampleLines(), deliveryAddressId(), chosen)));
        remoteOrdersPlaced().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void placeOrderWithUnknownOptionValueThrowsRejected() {
        List<SupplierOrderOption> options = assumeOrderOptions();
        SupplierProvider provider = providerFullyAvailable();
        Map<String, String> chosen = new LinkedHashMap<>(sampleOptions(provider, SupplierOrderOptionsContext.warehouse()));
        chosen.put(options.getFirst().key(), "tck-unknown-value");

        assertThrows(SupplierOrderRejectedException.class, () -> provider.placeOrder(
                new SupplierPurchaseRequest(uniqueClientOrderRef(), sampleLines(), deliveryAddressId(), chosen)));
        remoteOrdersPlaced().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void placeOrderWithSampleOptionsSucceeds() {
        assumeOrderOptions();
        SupplierProvider provider = providerFullyAvailable();

        SupplierOrderResult result = provider.placeOrder(purchaseRequest(provider, uniqueClientOrderRef(), sampleLines()));

        assertFalse(result.externalOrderId().isBlank());
    }

    private List<SupplierOrderOption> assumeOrderOptions() {
        List<SupplierOrderOption> options = providerFullyAvailable().orderOptions(SupplierOrderOptionsContext.warehouse());
        assumeTrue(!options.isEmpty(), "Supplier declares no order options");
        return options;
    }
}
