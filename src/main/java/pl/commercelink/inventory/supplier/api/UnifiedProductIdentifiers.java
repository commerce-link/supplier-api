package pl.commercelink.inventory.supplier.api;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UnifiedProductIdentifiers {

    private UnifiedProductIdentifiers() {
    }

    public static Set<String> unifyEans(Collection<String> eans) {
        return eans.stream().map(UnifiedProductIdentifiers::unifyEan).collect(Collectors.toSet());
    }

    public static String unifyEan(String ean) {
        if (ean != null && ean.startsWith("0")) {
            return ean.replaceFirst("0", "");
        }
        return ean;
    }

    public static Set<String> unifyMfns(Collection<String> mfns) {
        return mfns.stream().map(UnifiedProductIdentifiers::unifyMfn).collect(Collectors.toSet());
    }

    // remove any spaces from the codes
    public static String unifyMfn(String mfn) {
        if (mfn == null) {
            return null;
        }

        return mfn.trim()
                .replace(" ", "")
                .toUpperCase();
    }

    public static boolean areMfnsEq(String code1, String code2) {
        return Objects.equals(unifyMfn(code1), unifyMfn(code2));
    }

    public static boolean areEansEq(String ean1, String ean2) {
        return Objects.equals(unifyEan(ean1), unifyEan(ean2));
    }
}
