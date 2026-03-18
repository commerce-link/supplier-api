package pl.commercelink.inventory.supplier.api;

import java.util.Optional;

public interface CsvRowParser {

    ParsedRow parse(String[] row);

    default Optional<ParsedRow> tryParse(String[] row) {
        try {
            ParsedRow result = parse(row);
            if (result != null) {
                return Optional.of(result);
            }
        } catch (Exception ex) {
            System.out.println("Skipping row as it can't be parsed.");
        }
        return Optional.empty();
    }
}
