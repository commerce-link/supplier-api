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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Executable dropship contract for {@link SupplierProvider} implementations; every adapter that
 * answers {@code supportsDropshipping() == true} ships a test class extending this kit. Rules
 * mirror the ordering contract (idempotency per ref, all-or-nothing, ref guard, failures only as
 * {@link SupplierOrderException}) plus the consignee requirement; the shared rules are enforced
 * by the common base {@link SupplierPlacementContractTest}. They are documented in the
 * supplier-api README under "Dropship contract".
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

    protected final SupplierDropshipRequest dropshipRequest(String clientOrderRef, List<SupplierOrderLine> lines) {
        return new SupplierDropshipRequest(clientOrderRef, lines, sampleConsignee());
    }
}
