package pl.commercelink.inventory.supplier.api.ordering;

import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.support.ResourceDownloadException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static pl.commercelink.taxonomy.UnifiedProductIdentifiers.unifyEan;
import static pl.commercelink.taxonomy.UnifiedProductIdentifiers.unifyMfn;

public final class FeedCodeIndex implements ProductCodeResolver {

    private static final Pattern EAN_FORMAT = Pattern.compile("\\d{8,14}");
    private static final ConcurrentHashMap<String, CachedIndex> CACHE = new ConcurrentHashMap<>();

    public interface FeedSource {
        byte[] fetch() throws ResourceDownloadException;
    }

    private record Index(Map<String, SupplierProductCode> byEanAndMfn,
                         Map<String, SupplierProductCode> byEan) {}

    private record CachedIndex(long expiresAt, Index index) {}

    private final String supplierName;
    private final FeedSource feedSource;
    private final String cacheKey;
    private final Pattern separator;
    private final int codeColumn;
    private final int mfnColumn;
    private final int eanColumn;
    private final long timeToLiveMillis;
    private final Predicate<String> codeValidator;
    private final UnaryOperator<String> codeNormalizer;

    private FeedCodeIndex(Builder builder) {
        this.supplierName = builder.supplierName;
        this.feedSource = builder.feedSource;
        this.cacheKey = builder.supplierName + "|" + builder.cacheKey + "|" + builder.separator
                + "|" + builder.codeColumn + "|" + builder.mfnColumn + "|" + builder.eanColumn;
        this.separator = Pattern.compile(Pattern.quote(builder.separator));
        this.codeColumn = builder.codeColumn;
        this.mfnColumn = builder.mfnColumn;
        this.eanColumn = builder.eanColumn;
        this.timeToLiveMillis = builder.timeToLive.toMillis();
        this.codeValidator = builder.codeValidator;
        this.codeNormalizer = builder.codeNormalizer;
    }

    public static Builder builder(String supplierName, FeedSource feedSource) {
        return new Builder(supplierName, feedSource);
    }

    @Override
    public Optional<SupplierProductCode> resolve(SupplierOrderLine line) {
        Index index = index();
        SupplierProductCode exactMatch = index.byEanAndMfn().get(eanAndMfnKey(line.ean(), line.mfn()));
        SupplierProductCode match = exactMatch != null ? exactMatch : index.byEan().get(unifyEan(line.ean()));
        return Optional.ofNullable(match);
    }

    public Map<String, String> eanByCode() {
        Map<String, String> eanByCode = new HashMap<>();
        index().byEan().values().forEach(product -> eanByCode.putIfAbsent(product.code(), product.ean()));
        return eanByCode;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private Index index() {
        long now = System.currentTimeMillis();
        CACHE.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt());
        return CACHE.compute(cacheKey, (key, current) ->
                current != null && now < current.expiresAt()
                        ? current
                        : new CachedIndex(now + timeToLiveMillis, parse(fetchFeed()))).index();
    }

    private byte[] fetchFeed() {
        try {
            return feedSource.fetch();
        } catch (ResourceDownloadException e) {
            throw new SupplierOrderException(
                    "Failed to download " + supplierName + " feed for product code mapping", e);
        }
    }

    private Index parse(byte[] feed) {
        Map<String, SupplierProductCode> byEanAndMfn = new HashMap<>();
        Map<String, SupplierProductCode> byEan = new HashMap<>();
        int requiredColumns = Math.max(codeColumn, Math.max(mfnColumn, eanColumn));
        new String(feed, StandardCharsets.UTF_8).lines().forEach(row -> {
            String[] columns = separator.split(row);
            if (columns.length <= requiredColumns) return;
            String code = columns[codeColumn].trim();
            String rawEan = columns[eanColumn].trim();
            if (!codeValidator.test(code) || !EAN_FORMAT.matcher(rawEan).matches()) return;
            SupplierProductCode product = new SupplierProductCode(
                    codeNormalizer.apply(code), unifyEan(rawEan), columns[mfnColumn].trim());
            byEanAndMfn.putIfAbsent(eanAndMfnKey(rawEan, product.mfn()), product);
            byEan.putIfAbsent(product.ean(), product);
        });
        return new Index(byEanAndMfn, byEan);
    }

    private static String eanAndMfnKey(String ean, String mfn) {
        return unifyEan(ean) + "|" + unifyMfn(mfn);
    }

    public static final class Builder {

        private final String supplierName;
        private final FeedSource feedSource;
        private String cacheKey = "";
        private String separator = ";";
        private int codeColumn = -1;
        private int mfnColumn = -1;
        private int eanColumn = -1;
        private Duration timeToLive = Duration.ofMinutes(15);
        private Predicate<String> codeValidator = code -> !code.isBlank();
        private UnaryOperator<String> codeNormalizer = UnaryOperator.identity();

        private Builder(String supplierName, FeedSource feedSource) {
            this.supplierName = supplierName;
            this.feedSource = feedSource;
        }

        public Builder columns(int codeColumn, int mfnColumn, int eanColumn) {
            this.codeColumn = codeColumn;
            this.mfnColumn = mfnColumn;
            this.eanColumn = eanColumn;
            return this;
        }

        public Builder cacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
            return this;
        }

        public Builder separator(String separator) {
            this.separator = separator;
            return this;
        }

        public Builder timeToLive(Duration timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public Builder codeValidator(Predicate<String> codeValidator) {
            this.codeValidator = codeValidator;
            return this;
        }

        public Builder codeNormalizer(UnaryOperator<String> codeNormalizer) {
            this.codeNormalizer = codeNormalizer;
            return this;
        }

        public FeedCodeIndex build() {
            if (codeColumn < 0 || mfnColumn < 0 || eanColumn < 0) {
                throw new IllegalStateException("Feed columns are not configured");
            }
            return new FeedCodeIndex(this);
        }
    }
}
