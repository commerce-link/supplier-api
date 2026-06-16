package pl.commercelink.inventory.supplier.api;

public record SupplierConfigField(
        String key,
        String label,
        FieldType type,
        boolean required,
        String placeholder
) {
    public enum FieldType { TEXT, PASSWORD, URL, NUMBER }

    public static SupplierConfigField url() {
        return new SupplierConfigField("url", "Feed URL", FieldType.URL, true, "https://...");
    }

    public static SupplierConfigField token() {
        return new SupplierConfigField("token", "API Token", FieldType.PASSWORD, true, "");
    }

    public static SupplierConfigField host() {
        return new SupplierConfigField("host", "Host", FieldType.TEXT, true, "");
    }

    public static SupplierConfigField port() {
        return new SupplierConfigField("port", "Port", FieldType.NUMBER, true, "22");
    }

    public static SupplierConfigField username() {
        return new SupplierConfigField("username", "Username", FieldType.TEXT, true, "");
    }

    public static SupplierConfigField password() {
        return new SupplierConfigField("password", "Password", FieldType.PASSWORD, true, "");
    }
}
