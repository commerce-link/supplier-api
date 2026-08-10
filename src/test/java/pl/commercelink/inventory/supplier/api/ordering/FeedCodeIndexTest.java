package pl.commercelink.inventory.supplier.api.ordering;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.commercelink.inventory.supplier.api.SupplierOrderException;
import pl.commercelink.inventory.supplier.api.SupplierOrderLine;
import pl.commercelink.inventory.supplier.api.support.ResourceDownloadException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedCodeIndexTest {

    private static final String EAN_A = "4006381333931";
    private static final String EAN_B = "5901234123457";
    private static final String FEED = """
            Code;Mfn;Vendor;Catalog;Name;EAN
            101;MFN-A;BrandA;Cat;Product A;%s
            102;MFN-B;BrandB;Cat;Product B;%s
            103;MFN-A2;BrandA;Cat;Product A rev2;%s
            """.formatted(EAN_A, EAN_B, EAN_A);

    @BeforeEach
    void clearCache() {
        FeedCodeIndex.clearCache();
    }

    private FeedCodeIndex.Builder builder(String feed) {
        return FeedCodeIndex.builder("TestSupplier", () -> feed.getBytes(StandardCharsets.UTF_8))
                .columns(0, 1, 5);
    }

    @Test
    void resolvesCodeByEan() {
        // when
        Optional<SupplierProductCode> product = builder(FEED).build()
                .resolve(new SupplierOrderLine(EAN_B, "MFN-B", 1));

        // then
        assertEquals(Optional.of(new SupplierProductCode("102", EAN_B, "MFN-B")), product);
    }

    @Test
    void prefersExactEanAndMfnMatchForDuplicateEan() {
        // when
        Optional<SupplierProductCode> product = builder(FEED).build()
                .resolve(new SupplierOrderLine(EAN_A, "MFN-A2", 1));

        // then
        assertEquals(Optional.of(new SupplierProductCode("103", EAN_A, "MFN-A2")), product);
    }

    @Test
    void fallsBackToEanOnlyMatchForUnknownMfn() {
        // when
        Optional<SupplierProductCode> product = builder(FEED).build()
                .resolve(new SupplierOrderLine(EAN_B, "MFN-OTHER", 1));

        // then
        assertEquals(Optional.of(new SupplierProductCode("102", EAN_B, "MFN-B")), product);
    }

    @Test
    void failsClosedForUnknownEan() {
        // when
        Optional<SupplierProductCode> product = builder(FEED).build()
                .resolve(new SupplierOrderLine("9999999999999", "MFN-X", 1));

        // then
        assertEquals(Optional.empty(), product);
    }

    @Test
    void matchesEanIgnoringLeadingZeros() {
        // when
        Optional<SupplierProductCode> product = builder(FEED).build()
                .resolve(new SupplierOrderLine("0" + EAN_B, "MFN-B", 1));

        // then
        assertEquals(Optional.of(new SupplierProductCode("102", EAN_B, "MFN-B")), product);
    }

    @Test
    void ignoresRowsWithColumnShiftedEan() {
        // given
        String feedWithBrokenRow = FEED + "104;MFN-D;BrandD;Cat;Name; fragment;1111222233334\n";

        // when
        Optional<SupplierProductCode> product = builder(feedWithBrokenRow).build()
                .resolve(new SupplierOrderLine("1111222233334", "MFN-D", 1));

        // then
        assertEquals(Optional.empty(), product);
    }

    @Test
    void ignoresRowsFailingCodeValidator() {
        // given
        String feed = "abc;MFN-A;BrandA;Cat;Product A;" + EAN_A + "\n";
        FeedCodeIndex index = builder(feed)
                .codeValidator(code -> code.chars().allMatch(Character::isDigit))
                .build();

        // when / then
        assertEquals(Optional.empty(), index.resolve(new SupplierOrderLine(EAN_A, "MFN-A", 1)));
    }

    @Test
    void ignoresRowsWithTooFewColumns() {
        // given
        String feed = "101;MFN-A;BrandA\n102;MFN-B;BrandB;Cat;Product B;" + EAN_B + "\n";

        // when
        FeedCodeIndex index = builder(feed).build();

        // then
        assertEquals(Optional.empty(), index.resolve(new SupplierOrderLine(EAN_A, "MFN-A", 1)));
        assertTrue(index.resolve(new SupplierOrderLine(EAN_B, "MFN-B", 1)).isPresent());
    }

    @Test
    void supportsCustomSeparator() {
        // given
        String feed = "101,MFN-A,BrandA,Cat,Product A," + EAN_A + "\n";
        FeedCodeIndex index = builder(feed).separator(",").build();

        // when / then
        assertEquals(Optional.of(new SupplierProductCode("101", EAN_A, "MFN-A")),
                index.resolve(new SupplierOrderLine(EAN_A, "MFN-A", 1)));
    }

    @Test
    void mapsEanByCode() {
        // when
        Map<String, String> eanByCode = builder(FEED).build().eanByCode();

        // then
        assertEquals(EAN_B, eanByCode.get("102"));
        assertEquals(EAN_A, eanByCode.get("101"));
    }

    @Test
    void cachesFeedPerCacheKeyWithinTimeToLive() {
        // given
        AtomicInteger fetches = new AtomicInteger();
        FeedCodeIndex.FeedSource countingSource = () -> {
            fetches.incrementAndGet();
            return FEED.getBytes(StandardCharsets.UTF_8);
        };
        FeedCodeIndex first = FeedCodeIndex.builder("TestSupplier", countingSource)
                .columns(0, 1, 5).cacheKey("token-1").build();
        FeedCodeIndex second = FeedCodeIndex.builder("TestSupplier", countingSource)
                .columns(0, 1, 5).cacheKey("token-1").build();

        // when
        first.resolve(new SupplierOrderLine(EAN_A, "MFN-A", 1));
        second.resolve(new SupplierOrderLine(EAN_B, "MFN-B", 1));

        // then
        assertEquals(1, fetches.get());
    }

    @Test
    void loadsFeedSeparatelyPerCacheKey() {
        // given
        AtomicInteger fetches = new AtomicInteger();
        FeedCodeIndex.FeedSource countingSource = () -> {
            fetches.incrementAndGet();
            return FEED.getBytes(StandardCharsets.UTF_8);
        };
        FeedCodeIndex first = FeedCodeIndex.builder("TestSupplier", countingSource)
                .columns(0, 1, 5).cacheKey("token-1").build();
        FeedCodeIndex second = FeedCodeIndex.builder("TestSupplier", countingSource)
                .columns(0, 1, 5).cacheKey("token-2").build();

        // when
        first.resolve(new SupplierOrderLine(EAN_A, "MFN-A", 1));
        second.resolve(new SupplierOrderLine(EAN_B, "MFN-B", 1));

        // then
        assertEquals(2, fetches.get());
    }

    @Test
    void reloadsFeedAfterTimeToLiveExpires() throws InterruptedException {
        // given
        AtomicInteger fetches = new AtomicInteger();
        FeedCodeIndex.FeedSource countingSource = () -> {
            fetches.incrementAndGet();
            return FEED.getBytes(StandardCharsets.UTF_8);
        };
        FeedCodeIndex.Builder shortLived = FeedCodeIndex.builder("TestSupplier", countingSource)
                .columns(0, 1, 5).cacheKey("token-1").timeToLive(Duration.ofMillis(20));

        // when
        shortLived.build().resolve(new SupplierOrderLine(EAN_A, "MFN-A", 1));
        Thread.sleep(40);
        shortLived.build().resolve(new SupplierOrderLine(EAN_A, "MFN-A", 1));

        // then
        assertEquals(2, fetches.get());
    }

    @Test
    void wrapsFeedDownloadFailureInSupplierOrderException() {
        // given
        FeedCodeIndex index = FeedCodeIndex.builder("TestSupplier",
                        () -> { throw new ResourceDownloadException("connection refused", new RuntimeException()); })
                .columns(0, 1, 5).build();

        // when
        SupplierOrderException exception = assertThrows(SupplierOrderException.class,
                () -> index.resolve(new SupplierOrderLine(EAN_A, "MFN-A", 1)));

        // then
        assertEquals("Failed to download TestSupplier feed for product code mapping", exception.getMessage());
    }

    @Test
    void rejectsBuilderWithoutColumns() {
        // when / then
        assertThrows(IllegalStateException.class,
                () -> FeedCodeIndex.builder("TestSupplier", FEED::getBytes).build());
    }
}
