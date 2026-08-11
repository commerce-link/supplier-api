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

### Contract test kit

The contract is executable: `api.testing.SupplierOrderingContractTest` is an abstract JUnit 5
class published in the main artifact (its JUnit dependency is `provided`-scoped, so it adds
nothing to the runtime classpath; the main artifact was chosen over a test-jar so the shared CI
publish workflow needs no changes). Every ordering adapter extends it in its own test suite:

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
`unknownProductLine()`, and the observability counters `remoteOrdersPlaced()` / `remoteCalls()`.
Tests for optional hooks left at their defaults are skipped with an assumption message.

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
- **`OrderingValues`** — conservative stock-quantity parsing (`"1 000+"` → 1000, non-numeric →
  0), currency fallback, URL-safe path encoding of client refs.

A typical new adapter is then a descriptor, its wire DTOs, and the mapping hooks above — see
`supplier-elko` for the reference implementation and `supplier-acme` for the dev mock.
