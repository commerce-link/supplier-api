# Supplier API

Shared interfaces and data types for the CommerceLink supplier feed plugin system.

This module defines the contracts that all supplier implementations (AbGroup, Action, Also, etc.) build on:

- **`SupplierProviderDescriptor`** — provider-api descriptor every supplier implements (extends `ProviderDescriptor<SupplierProvider>`). Declares `create(config)`, `feedFormat()`, `supplierInfo()`, and `configurationFields()`.
- **`SupplierProvider`** — config-bound runtime produced by `SupplierProviderDescriptor.create(config)`. Declares `download()`.
- **`InventoryItem`** — core inventory record (EAN, MFN, price, quantity, supplier).
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
