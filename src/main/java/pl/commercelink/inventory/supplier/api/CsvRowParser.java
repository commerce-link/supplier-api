package pl.commercelink.inventory.supplier.api;

import java.util.Optional;

public interface CsvRowParser {

    ParsedRow parse(String[] row);

    default Optional<ParsedRow> tryParse(String[] row) {
        try {
            return Optional.ofNullable(parse(row));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
