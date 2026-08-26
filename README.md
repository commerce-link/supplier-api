# Supplier API

Shared interfaces and data types for the CommerceLink supplier feed plugin system.

This module defines the contracts that all supplier implementations (AbGroup, Action, Also, etc.) build on:

- **`SupplierProviderDescriptor`** — provider-api descriptor every supplier implements (extends `ProviderDescriptor<SupplierProvider>`). Declares `create(config)`, `feedFormat()`, `supplierInfo()`, and `configurationFields()`.
- **`SupplierProvider`** — config-bound runtime produced by `SupplierProviderDescriptor.create(config)`. Declares `download()`.
- **`InventoryItem`** — core inventory record (EAN, MFN, price, quantity, supplier), plus the
  supplier's own product code (`sku`), filled by the feed parser and left unnormalized.
- **`Taxonomy`** — product taxonomy record (brand, name, category) extracted from supplier feeds.
- **`FeedFormat`** — sealed interface discriminating CSV and XML feed formats.
- **`CsvRowParser`** / **`XmlItem`** — parser contracts for feed data.
- **`SupplierInfo`** — supplier metadata (name, type, origin, shipping policy).

Support utilities (`api.support` package):

- **`HttpFileDownloader`**, **`FtpFileDownloader`**, **`SftpFileDownloader`** — feed download transports.
- **`FileZipper`** — ZIP/GZIP decompression.
- **`ProductFeedPurifier`** — CSV data cleanup.
- **`ProductCategoryMapper`** — base class for supplier-specific category mapping.

## Usage

Supplier implementations depend on this artifact and implement `SupplierProviderDescriptor`:

```java
public class MySupplierProviderDescriptor implements SupplierProviderDescriptor {
    public SupplierProvider create(Map<String, String> config) {
        return () -> { /* download the feed using config */ };
    }
    public FeedFormat feedFormat() { ... }
    public SupplierInfo supplierInfo() { ... }
}
```

## Ordering contract

Providers that return `true` from `supportsOrdering()` implement `checkAvailability(lines)` and
`placeOrder(request)`. The contract every ordering adapter must satisfy:

1. **Idempotency** — `placeOrder` retried with the same `clientOrderRef` does not place a second
   order at the supplier; the retry returns the already-placed order.
2. **All-or-nothing** — any shortage fails the whole order with `SupplierOrderException`; no
   partial purchase order is placed.
3. **Order id required** — a blank or missing `externalOrderId` in the supplier response raises
   `SupplierOrderException` (an order without a traceable id cannot be reconciled).
4. **Ref guard** — a blank or missing `clientOrderRef` raises `SupplierOrderException` before any
   remote call (a blank ref would silently disable idempotency).
5. **Single failure type** — all failures surface only as `SupplierOrderException`, never as raw
   HTTP or client exceptions; the application's error handling keys on the SPI type.
6. **Fail closed** — `checkAvailability` quotes quantity 0 for an unknown product or a missing
   price, never an optimistic guess.
7. **Missing sku fails closed** — an order line whose `sku` is null is quoted quantity 0 by
   `checkAvailability` without a remote call for that line, and rejected by `placeOrder` with
   `SupplierOrderException` naming the EAN before any ordering call.
8. **Delivery address fails closed** — a provider returning `true` from
   `requiresDeliveryAddress()` lists the account's addresses in `deliveryAddresses()` and raises
   `SupplierOrderException` when the list cannot be fetched or comes back empty, and when
   `placeOrder` is called without a `deliveryAddressId`. Shipping to a guessed address is worse
   than not ordering, so the application blocks the purchase instead of falling back.

### Contract test kit

The contract is executable: `api.testing.SupplierOrderingContractTest` is an abstract JUnit 5
class published in the main artifact (its JUnit dependency is `provided`-scoped, so it adds
nothing to the runtime classpath; the main artifact was chosen over a test-jar so the shared CI
publish workflow needs no changes). Rules 1, 2, 4 and 5 (idempotency, all-or-nothing, ref guard,
single failure type) are shared with the dropship contract and enforced by the common base
`api.testing.SupplierPlacementContractTest`, which this kit extends. Every ordering adapter
extends it in its own test suite:

```java
class MyOrderingContractTest extends SupplierOrderingContractTest {
    protected SupplierProvider providerFullyAvailable() { ... }
    protected SupplierProvider providerWithShortage() { ... }
    protected List<SupplierOrderLine> sampleLines() { ... }
    protected String uniqueClientOrderRef() { return UUID.randomUUID().toString(); }
}
```

The four required hooks build providers backed by the adapter's own fixtures (a static feed, a
stubbed HTTP client). Optional hooks unlock the remaining scenarios and stricter assertions:
`providerReturningBlankOrderId()`, `providerWithFailingBackend()`, `providerWithMissingPrice()`,
`unknownProductLine()`, `deliveryAddressId()`, and the observability counters
`remoteOrdersPlaced()` / `remoteCalls()`. Tests for optional hooks left at their defaults are
skipped with an assumption message. Adapters requiring a delivery address override
`deliveryAddressId()` with a value their fixture accepts — the kit builds every purchase request
through it and additionally checks the address rules from contract point 8.

## Dropship contract

Providers that return `true` from `supportsDropshipping()` implement
`placeDropshipOrder(request)`. Dropshipping is deliberately a separate capability from
`supportsOrdering()`: real suppliers expose it as a different endpoint with its own contract
(e.g. ELKO `POST /Orders/EndUser` vs `POST /Orders`) and it may hinge on a separate business
agreement (ELKO requires a signed B2C agreement), so an adapter can support either capability
without the other.

Rules 1-7 of the ordering contract apply unchanged to `placeDropshipOrder` (idempotency per
`clientOrderRef`, all-or-nothing, order id required, ref guard, single failure type, fail
closed, missing sku fails closed). Instead of rule 8, dropshipping adds:

8a. **Consignee required** — a `SupplierDropshipRequest` without a consignee raises
   `SupplierOrderException` before any remote call. `SupplierConsignee` itself refuses to be
   constructed without street/postal/city/country, a company or full personal name, a phone and
   an email — carriers notify the end customer directly, so incomplete contact data would
   surface as a delivery failure days later.

9. **Outcome classes** — a dropship placement follows the same failure classes as `placeOrder`:
   a definite pre-send failure (availability, basket, address creation) raises
   `SupplierOrderRejectedException`; a failure once the placement request may have left the
   process raises `SupplierOrderOutcomeUnknownException` (the engine does this for any other
   exception thrown by `placeNewDropshipOrder`). `findPlacedOrder` probes the dropship lookup
   too, so reconcile finds dropship orders living in a separate supplier view.
10. **Pickup points** — `SupplierDropshipRequest.pickupPoint` (`SupplierPickupPoint`: canonical
   carrier name + point code, optional address) asks the supplier to deliver to a carrier point.
   Providers declare support with `supportsPickupPointDropship()` (default `false`); a request
   carrying a pickup point to a provider without support raises `SupplierOrderRejectedException`
   before any remote call — the parcel is never redirected to the street address. The consignee
   stays mandatory (recipient name and contact data for the carrier notification).

The contract is executable: adapters extend `api.testing.SupplierDropshipContractTest`
(required hooks `dropshipProvider()`, `dropshipProviderWithShortage()`, `sampleLines()`,
`uniqueClientOrderRef()`; optional `sampleConsignee()`, `dropshipProviderWithFailingBackend()`,
`remoteDropshipOrdersPlaced()`, `remoteCalls()`,
`dropshipProviderWithPlacementTransportFailure()`, `dropshipProviderRejectingOrders()`,
`samplePickupPoint()`, `dropshipProviderWithoutPickupPoints()`). Like the ordering kit, it
extends the common base `api.testing.SupplierPlacementContractTest`, which enforces the shared
placement rules (idempotency, all-or-nothing, ref guard, single failure type).

## Order tracking contract

Providers that return `true` from `supportsOrderTracking()` implement
`trackOrder(SupplierOrderLookup)`: a **read-only** snapshot of an order previously placed at the
supplier — `SupplierOrderTracking(state, parcels)` with `state` in
`PROCESSING | PARTIALLY_SHIPPED | SHIPPED | CANCELLED` and one `SupplierParcel` per parcel handed
to a carrier (`carrier` and `trackingNo` mandatory, `trackingUrl`/`shippedAt` optional, `lines`
only when the supplier reports the split — an empty list means "everything still outstanding").

Rules:

1. **Read-only** — `trackOrder` never places, changes or cancels anything.
2. **Lookup by whichever id the API indexes** — `SupplierOrderLookup` carries the supplier's
   `externalOrderId` and our `clientOrderRef`; at least one is present (the record rejects an
   empty lookup before any remote call).
3. **Unknown → empty** — an order the supplier does not (yet) see yields `Optional.empty()`, not
   an exception.
4. **Single failure type** — communication failures surface only as `SupplierOrderException`.
5. **No delivered date** — delivery to the end customer is out of scope; the application keeps it
   manual.

The contract is executable: adapters extend `api.testing.SupplierOrderTrackingContractTest`
(required hooks `trackingProvider()`, `sampleLines()`, `uniqueClientOrderRef()`,
`placeSampleOrder(provider, ref)`; optional `advanceToShipped(provider, ref, externalOrderId)`,
`trackingProviderWithFailingBackend()`, `remotePlacedOrders()`).

### Ordering building blocks (`api.ordering` package)

Common mechanics extracted from real adapters — the parts that repeat per supplier, deliberately
excluding HTTP contract mapping (DTOs, endpoints, auth, error semantics), which stays explicit
per adapter:

- **Sku flow** — the feed parser writes the supplier's product code into `InventoryItem.sku`
  (normalizing it per supplier as needed); the application looks the sku up from its own
  inventory when building `SupplierOrderLine`s; adapters read `line.sku()` directly when calling
  the supplier's ordering API. No feed re-download and no resolver layer are needed at order
  time. `FeedSource` (in `api.support`) is the injectable feed-bytes seam adapters use for
  `download()` and for tests.
- **`IdempotentOrderPlacement<L, O>`** — skeleton for `placeOrder` owning the universal
  sequence: blank-ref guard → line translation → per-ref lock → replay lookup → placement →
  order id verification → result mapping, with every failure wrapped in
  `SupplierOrderException`. The adapter fills the supplier-specific holes: `toSupplierLine`,
  `findExistingOrder`, `placeNewOrder`, `externalOrderId`, `toResult`.
  The dropship twin `placeDropshipIdempotently` runs the same sequence for
  `SupplierDropshipRequest` (consignee guard included) with hooks
  `findExistingDropshipOrder` (defaults to `findExistingOrder`),
  `placeNewDropshipOrder` and `toDropshipResult` (defaults to `toResult`).
- **`OrderingValues`** — conservative stock-quantity parsing (`"1 000+"` → 1000, non-numeric →
  0), currency fallback, URL-safe path encoding of client refs.

A typical new adapter is then a descriptor, its wire DTOs, and the mapping hooks above — see
`supplier-elko` for the reference implementation and `supplier-acme` for the dev mock.
