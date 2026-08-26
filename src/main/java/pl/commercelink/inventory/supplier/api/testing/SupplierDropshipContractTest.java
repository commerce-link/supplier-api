package pl.commercelink.inventory.supplier.api.testing;

import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.FeedData;
import pl.commercelink.inventory.supplier.api.SupplierConsignee;
import pl.commercelink.inventory.supplier.api.SupplierDropshipRequest;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderOption;
import pl.commercelink.inventory.supplier.api.SupplierOrderOptionsContext;
import pl.commercelink.inventory.supplier.api.SupplierOrderOutcomeUnknownException;
import pl.commercelink.inventory.supplier.api.SupplierOrderRejectedException;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierPickupPoint;
import pl.commercelink.inventory.supplier.api.SupplierProvider;

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
 * Executable dropship contract for {@link SupplierProvider} implementations; every adapter that
 * answers {@code supportsDropshipping() == true} ships a test class extending this kit. Rules
 * mirror the ordering contract (idempotency per ref, all-or-nothing, ref guard, failures only as
 * {@link SupplierOrderException}) plus the consignee requirement; the shared rules are enforced
 * by the common base {@link SupplierPlacementContractTest}. Placement failures classify as either
 * {@link SupplierOrderRejectedException} (definite pre-send rejection) or
 * {@link SupplierOrderOutcomeUnknownException} (failure once the request may have left the
 * process). Pickup-point rules: a request carrying {@link SupplierPickupPoint} is honoured only
 * when {@link SupplierProvider#supportsPickupPointDropship()} answers {@code true}, is rejected
 * with {@link SupplierOrderRejectedException} before any remote call otherwise, and an adapter
 * that supports pickup points but cannot resolve the requested code never falls back to courier
 * delivery. They are documented in the supplier-api README under "Dropship contract".
 */
public abstract class SupplierDropshipContractTest extends SupplierPlacementContractTest {

    /** Provider able to dropship every line of {@link #sampleLines()}. */
    protected abstract SupplierProvider dropshipProvider();

    /** Provider short on at least one line of {@link #sampleLines()}. */
    protected abstract SupplierProvider dropshipProviderWithShortage();

    /** End customer the adapter's fixture accepts. */
    protected SupplierConsignee sampleConsignee() {
        return new SupplierConsignee(null, "Jan", "Kowalski",
                "ul. Polna 1", "00-001", "Warszawa", "PL", "+48601234567", "jan.kowalski@example.com");
    }

    /** Provider whose backend fails every remote call with a raw transport error. */
    protected Optional<SupplierProvider> dropshipProviderWithFailingBackend() {
        return Optional.empty();
    }

    /** Number of dropship orders actually placed at the (fake) supplier since the test started, if observable. */
    protected OptionalInt remoteDropshipOrdersPlaced() {
        return OptionalInt.empty();
    }

    /** Provider whose dropship placement call dies mid-flight (order may or may not exist). */
    protected Optional<SupplierProvider> dropshipProviderWithPlacementTransportFailure() {
        return Optional.empty();
    }

    /** Provider whose backend definitively rejects the dropship order before it exists. */
    protected Optional<SupplierProvider> dropshipProviderRejectingOrders() {
        return Optional.empty();
    }

    /** A pickup point {@link #dropshipProvider()} accepts; empty when the adapter has no pickup support. */
    protected Optional<SupplierPickupPoint> samplePickupPoint() {
        return Optional.empty();
    }

    /** Provider of the same adapter that does NOT support pickup points, for the guard test. */
    protected Optional<SupplierProvider> dropshipProviderWithoutPickupPoints() {
        return Optional.empty();
    }

    /**
     * The pickup-point code the (fake) supplier received for the most recent dropship order, if
     * observable.
     */
    protected Optional<String> remotePickupPointCode() {
        return Optional.empty();
    }

    @Override
    protected final SupplierProvider placementProvider() {
        return dropshipProvider();
    }

    @Override
    protected final SupplierProvider placementProviderWithShortage() {
        return dropshipProviderWithShortage();
    }

    @Override
    protected final Optional<SupplierProvider> placementProviderWithFailingBackend() {
        return dropshipProviderWithFailingBackend();
    }

    @Override
    protected final OptionalInt remotePlacedOrders() {
        return remoteDropshipOrdersPlaced();
    }

    @Override
    protected final boolean supportsPlacement(SupplierProvider provider) {
        return provider.supportsDropshipping();
    }

    @Override
    protected final SupplierOrderResult place(SupplierProvider provider, String clientOrderRef,
                                              List<SupplierOrderLine> lines) {
        return provider.placeDropshipOrder(dropshipRequest(clientOrderRef, lines));
    }

    @Test
    void nullConsigneeThrowsBeforeAnyRemoteCall() {
        // given
        SupplierProvider provider = dropshipProvider();

        // when / then
        assertThrows(SupplierOrderException.class, () -> provider.placeDropshipOrder(
                new SupplierDropshipRequest(uniqueClientOrderRef(), sampleLines(), null)));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void defaultProviderDoesNotSupportDropshipping() {
        // given
        SupplierProvider defaults = Optional::<FeedData>empty;

        // when / then
        assertFalse(defaults.supportsDropshipping());
        assertThrows(UnsupportedOperationException.class, () -> defaults.placeDropshipOrder(
                dropshipRequest(uniqueClientOrderRef(), sampleLines())));
    }

    @Test
    void placementTransportFailureSurfacesAsOutcomeUnknown() {
        // given
        SupplierProvider provider = assumePresent(dropshipProviderWithPlacementTransportFailure(),
                "a placement transport failure");

        // when / then
        assertThrows(SupplierOrderOutcomeUnknownException.class,
                () -> provider.placeDropshipOrder(dropshipRequest(uniqueClientOrderRef(), sampleLines())));
    }

    @Test
    void explicitRejectionSurfacesAsRejected() {
        // given
        SupplierProvider provider = assumePresent(dropshipProviderRejectingOrders(), "an explicit rejection");

        // when / then
        assertThrows(SupplierOrderRejectedException.class,
                () -> provider.placeDropshipOrder(dropshipRequest(uniqueClientOrderRef(), sampleLines())));
    }

    @Test
    void pickupPointOrderIsPlacedWhenSupported() {
        // given
        SupplierPickupPoint point = samplePickupPoint().orElse(null);
        assumeTrue(point != null, "Adapter does not support pickup points — hook not implemented");
        SupplierProvider provider = dropshipProvider();
        String ref = uniqueClientOrderRef();
        Map<String, String> options = sampleOptions(provider, SupplierOrderOptionsContext.dropship(point));

        // when
        SupplierOrderResult first = provider.placeDropshipOrder(
                new SupplierDropshipRequest(ref, sampleLines(), sampleConsignee(), null, point, options));
        remotePickupPointCode().ifPresent(code -> assertEquals(point.code(), code));
        SupplierOrderResult retry = provider.placeDropshipOrder(
                new SupplierDropshipRequest(ref, sampleLines(), sampleConsignee(), null, point, options));

        // then
        assertTrue(provider.supportsPickupPointDropship());
        assertFalse(first.externalOrderId().isBlank());
        assertEquals(first.externalOrderId(), retry.externalOrderId());
    }

    @Test
    void pickupPointRequestToProviderWithoutSupportIsRejectedBeforeAnyRemoteCall() {
        // given
        SupplierProvider provider = assumePresent(dropshipProviderWithoutPickupPoints(), "a provider without pickup points");
        SupplierPickupPoint point = new SupplierPickupPoint("InPost", "WAW04A", null, null, null, null);

        // when / then
        assertFalse(provider.supportsPickupPointDropship());
        assertThrows(SupplierOrderRejectedException.class, () -> provider.placeDropshipOrder(
                new SupplierDropshipRequest(uniqueClientOrderRef(), sampleLines(), sampleConsignee(), null, point)));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
    }

    protected final SupplierDropshipRequest dropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines) {
        return new SupplierDropshipRequest(clientOrderRef, lines, sampleConsignee(), null, null,
                sampleOptions(dropshipProvider(), SupplierOrderOptionsContext.dropship(null)));
    }

    @Test
    void dropshipOrderOptionsAreWellFormed() {
        List<SupplierOrderOption> options = assumeDropshipOrderOptions();

        assertEquals(options.size(), options.stream().map(SupplierOrderOption::key).distinct().count());
        options.forEach(option -> assertFalse(option.choices().isEmpty(), option.key() + " has no choices"));
    }

    @Test
    void placeDropshipWithoutRequiredOptionThrowsRejected() {
        List<SupplierOrderOption> options = assumeDropshipOrderOptions();
        SupplierOrderOption required = options.stream().filter(SupplierOrderOption::required).findFirst()
                .orElseGet(() -> { assumeTrue(false, "No required option declared"); return null; });
        SupplierProvider provider = dropshipProvider();
        Map<String, String> chosen = new LinkedHashMap<>(
                sampleOptions(provider, SupplierOrderOptionsContext.dropship(null)));
        chosen.remove(required.key());

        assertThrows(SupplierOrderRejectedException.class, () -> provider.placeDropshipOrder(
                new SupplierDropshipRequest(uniqueClientOrderRef(), sampleLines(), sampleConsignee(), null, null, chosen)));
        remoteDropshipOrdersPlaced().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void placeDropshipWithUnknownOptionValueThrowsRejected() {
        List<SupplierOrderOption> options = assumeDropshipOrderOptions();
        SupplierProvider provider = dropshipProvider();
        Map<String, String> chosen = new LinkedHashMap<>(
                sampleOptions(provider, SupplierOrderOptionsContext.dropship(null)));
        chosen.put(options.getFirst().key(), "tck-unknown-value");

        assertThrows(SupplierOrderRejectedException.class, () -> provider.placeDropshipOrder(
                new SupplierDropshipRequest(uniqueClientOrderRef(), sampleLines(), sampleConsignee(), null, null, chosen)));
        remoteDropshipOrdersPlaced().ifPresent(count -> assertEquals(0, count));
    }

    private List<SupplierOrderOption> assumeDropshipOrderOptions() {
        List<SupplierOrderOption> options = dropshipProvider().orderOptions(SupplierOrderOptionsContext.dropship(null));
        assumeTrue(!options.isEmpty(), "Supplier declares no order options");
        return options;
    }
}
