package pl.commercelink.inventory.supplier.api.testing;

import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.FeedData;
import pl.commercelink.inventory.supplier.api.SupplierConsignee;
import pl.commercelink.inventory.supplier.api.SupplierDropshipRequest;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierProvider;

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
 * Executable dropship contract for {@link SupplierProvider} implementations; every adapter that
 * answers {@code supportsDropshipping() == true} ships a test class extending this kit. Rules
 * mirror the ordering contract (idempotency per ref, all-or-nothing, failures only as
 * {@link SupplierOrderException}) plus the consignee requirement; they are documented in the
 * supplier-api README under "Dropship contract".
 */
public abstract class SupplierDropshipContractTest {

    /** Provider able to dropship every line of {@link #sampleLines()}. */
    protected abstract SupplierProvider dropshipProvider();

    /** Provider short on at least one line of {@link #sampleLines()}. */
    protected abstract SupplierProvider dropshipProviderWithShortage();

    /** Order lines known to the adapter's fixture, each with a filled supplier sku and fully available from {@link #dropshipProvider()}. */
    protected abstract List<SupplierOrderLine> sampleLines();

    /** A client order reference never used before in this JVM (adapters may cache orders statically). */
    protected abstract String uniqueClientOrderRef();

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

    /** Number of remote backend interactions since the test started, if observable. */
    protected OptionalInt remoteCalls() {
        return OptionalInt.empty();
    }

    @Test
    void providerReportsDropshipSupport() {
        // when / then
        assertTrue(dropshipProvider().supportsDropshipping());
    }

    @Test
    void placeDropshipOrderReturnsExternalOrderId() {
        // given
        SupplierProvider provider = dropshipProvider();

        // when
        SupplierOrderResult result = provider.placeDropshipOrder(
                dropshipRequest(uniqueClientOrderRef(), sampleLines()));

        // then
        assertNotNull(result.externalOrderId());
        assertFalse(result.externalOrderId().isBlank());
    }

    @Test
    void retryWithSameRefDoesNotPlaceSecondOrder() {
        // given
        SupplierProvider provider = dropshipProvider();
        SupplierDropshipRequest request = dropshipRequest(uniqueClientOrderRef(), sampleLines());

        // when
        SupplierOrderResult first = provider.placeDropshipOrder(request);
        SupplierOrderResult second = provider.placeDropshipOrder(request);

        // then
        assertEquals(first.externalOrderId(), second.externalOrderId());
        assertEquals(first.totalNet(), second.totalNet());
        remoteDropshipOrdersPlaced().ifPresent(count -> assertEquals(1, count));
    }

    @Test
    void shortageThrowsWithoutPlacingPartialOrder() {
        // given
        SupplierProvider provider = dropshipProviderWithShortage();
        SupplierDropshipRequest request = dropshipRequest(uniqueClientOrderRef(), sampleLines());

        // when / then
        assertThrows(SupplierOrderException.class, () -> provider.placeDropshipOrder(request));
        remoteDropshipOrdersPlaced().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void blankRefThrowsBeforeAnyRemoteCall() {
        // given
        SupplierProvider provider = dropshipProvider();

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> provider.placeDropshipOrder(dropshipRequest(" ", sampleLines())));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void nullRefThrowsBeforeAnyRemoteCall() {
        // given
        SupplierProvider provider = dropshipProvider();

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> provider.placeDropshipOrder(dropshipRequest(null, sampleLines())));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
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
    void failureSurfacesOnlyAsSupplierOrderException() {
        // given
        SupplierProvider provider = assumePresent(dropshipProviderWithFailingBackend(), "a failing backend");

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> provider.placeDropshipOrder(dropshipRequest(uniqueClientOrderRef(), sampleLines())));
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

    protected final SupplierDropshipRequest dropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines) {
        return new SupplierDropshipRequest(clientOrderRef, lines, sampleConsignee());
    }

    private static SupplierProvider assumePresent(Optional<SupplierProvider> provider, String scenario) {
        assumeTrue(provider.isPresent(), "Adapter does not simulate " + scenario + " — hook not implemented");
        return provider.orElseThrow();
    }
}
