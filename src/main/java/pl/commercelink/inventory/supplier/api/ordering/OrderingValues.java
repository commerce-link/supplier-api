package pl.commercelink.inventory.supplier.api.ordering;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OrderingValues {

    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private OrderingValues() {
    }

    public static int parseStockQuantity(String stockQuantity) {
        if (stockQuantity == null) return 0;
        Matcher matcher = DIGITS.matcher(stockQuantity.replaceAll("[\\s\\u00A0,]", ""));
        if (!matcher.find()) return 0;
        try {
            return Integer.parseInt(matcher.group());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String currencyOrDefault(String currency, String defaultCurrency) {
        return currency == null || currency.isBlank() ? defaultCurrency : currency;
    }

    public static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
