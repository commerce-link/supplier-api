package pl.commercelink.inventory.supplier.api.testing;

import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.SupplierOrderOption;
import pl.commercelink.inventory.supplier.api.SupplierOrderOptionsContext;
import pl.commercelink.inventory.supplier.api.SupplierOrderResult;
import pl.commercelink.inventory.supplier.api.SupplierProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared placement rules for {@link SupplierProvider} implementations, common to both the
 * ordering and the dropship contract: a successful placement returns a non-blank external order
 * id, retrying with the same client order reference is idempotent, a shortage throws without
 * placing a partial order, a blank or null reference throws before any remote call, and every
 * failure surfaces only as {@link SupplierOrderException}. {@link SupplierOrderingContractTest}
 * and {@link SupplierDropshipContractTest} both extend this kit and adapt it to their own request
 * shape via the hook methods below.
 */
public abstract class SupplierPlacementContractTest {

    /** Provider able to fulfil every line of {@link #sampleLines()}. */
    protected abstract SupplierProvider placementProvider();

    /** Provider short on at least one line of {@link #sampleLines()}. */
    protected abstract SupplierProvider placementProviderWithShortage();

    /** Order lines known to the adapter's fixture, each with a filled supplier sku and fully orderable from {@link #placementProvider()}. */
    protected abstract List<SupplierOrderLine> sampleLines();

    /** A client order reference never used before in this JVM (adapters may cache orders statically). */
    protected abstract String uniqueClientOrderRef();

    /** Whether the given provider supports this placement kind. */
    protected abstract boolean supportsPlacement(SupplierProvider provider);

    /** Places an order for the given client order reference and lines. */
    protected abstract SupplierOrderResult place(SupplierProvider provider, String clientOrderRef,
                                                 List<SupplierOrderLine> lines);

    /** Provider whose backend fails every remote call with a raw transport error. */
    protected Optional<SupplierProvider> placementProviderWithFailingBackend() {
        return Optional.empty();
    }

    /** Number of orders actually placed at the (fake) supplier since the test started, if observable. */
    protected OptionalInt remotePlacedOrders() {
        return OptionalInt.empty();
    }

    /** Number of remote backend interactions since the test started, if observable. */
    protected OptionalInt remoteCalls() {
        return OptionalInt.empty();
    }

    /**
     * Answers for every option the provider declares: its default, else the first choice. Adapters
     * whose fixture needs different answers override this.
     */
    protected Map<String, String> sampleOptions(SupplierProvider provider, SupplierOrderOptionsContext context) {
        Map<String, String> chosen = new LinkedHashMap<>();
        for (SupplierOrderOption option : provider.orderOptions(context)) {
            String value = option.defaultValue() != null ? option.defaultValue()
                    : option.choices().isEmpty() ? null : option.choices().getFirst().value();
            if (value != null) {
                chosen.put(option.key(), value);
            }
        }
        return chosen;
    }

    @Test
    void providerReportsPlacementSupport() {
        // when / then
        assertTrue(supportsPlacement(placementProvider()));
    }

    @Test
    void placementReturnsExternalOrderId() {
        // given
        SupplierProvider provider = placementProvider();

        // when
        SupplierOrderResult result = place(provider, uniqueClientOrderRef(), sampleLines());

        // then
        assertNotNull(result.externalOrderId());
        assertFalse(result.externalOrderId().isBlank());
    }

    @Test
    void retryWithSameRefDoesNotPlaceSecondOrder() {
        // given
        SupplierProvider provider = placementProvider();
        String clientOrderRef = uniqueClientOrderRef();
        List<SupplierOrderLine> lines = sampleLines();

        // when
        SupplierOrderResult first = place(provider, clientOrderRef, lines);
        SupplierOrderResult second = place(provider, clientOrderRef, lines);

        // then
        assertEquals(first.externalOrderId(), second.externalOrderId());
        assertEquals(first.totalNet(), second.totalNet());
        remotePlacedOrders().ifPresent(count -> assertEquals(1, count));
    }

    @Test
    void shortageThrowsWithoutPlacingPartialOrder() {
        // given
        SupplierProvider provider = placementProviderWithShortage();
        String clientOrderRef = uniqueClientOrderRef();
        List<SupplierOrderLine> lines = sampleLines();

        // when / then
        assertThrows(SupplierOrderException.class, () -> place(provider, clientOrderRef, lines));
        remotePlacedOrders().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void blankRefThrowsBeforeAnyRemoteCall() {
        // given
        SupplierProvider provider = placementProvider();

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> place(provider, " ", sampleLines()));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void nullRefThrowsBeforeAnyRemoteCall() {
        // given
        SupplierProvider provider = placementProvider();

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> place(provider, null, sampleLines()));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void failureSurfacesOnlyAsSupplierOrderException() {
        // given
        SupplierProvider provider = assumePresent(placementProviderWithFailingBackend(), "a failing backend");

        // when / then
        assertThrows(SupplierOrderException.class,
                () -> place(provider, uniqueClientOrderRef(), sampleLines()));
    }

    protected static SupplierProvider assumePresent(Optional<SupplierProvider> provider, String scenario) {
        assumeTrue(provider.isPresent(), "Adapter does not simulate " + scenario + " — hook not implemented");
        return provider.orElseThrow();
    }
}
