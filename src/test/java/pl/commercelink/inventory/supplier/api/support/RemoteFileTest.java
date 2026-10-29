package pl.commercelink.inventory.supplier.api.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoteFileTest {

    private static final Pattern DATAPACK = Pattern.compile("Datapack_\\d+_\\d{4}-\\d{2}-\\d{2}\\.zip");

    @Test
    void picksTheMostRecentlyModifiedMatchingFile() {
        // given
        List<RemoteFile> files = List.of(
                new RemoteFile("Datapack_1014274_2026-10-28.zip", 1_761_600_000L),
                new RemoteFile("Datapack_1014274_2026-10-29.zip", 1_761_686_400L),
                new RemoteFile("Datapack_1014274_2026-10-27.zip", 1_761_513_600L));

        // when
        Optional<String> latest = RemoteFile.latest(files, DATAPACK);

        // then
        assertEquals(Optional.of("Datapack_1014274_2026-10-29.zip"), latest);
    }

    @Test
    void ignoresFilesNotMatchingThePattern() {
        // given
        List<RemoteFile> files = List.of(
                new RemoteFile(".", 1_761_700_000L),
                new RemoteFile("..", 1_761_700_000L),
                new RemoteFile("readme.txt", 1_761_700_000L),
                new RemoteFile("Datapack_1014274_2026-10-30.csv", 1_761_700_000L),
                new RemoteFile("Datapack_1014274_2026-10-28.zip", 1_761_600_000L));

        // when
        Optional<String> latest = RemoteFile.latest(files, DATAPACK);

        // then
        assertEquals(Optional.of("Datapack_1014274_2026-10-28.zip"), latest);
    }

    @Test
    void breaksATieInModificationTimeByTheName() {
        // given
        List<RemoteFile> files = List.of(
                new RemoteFile("Datapack_1014274_2026-10-29.zip", 1_761_686_400L),
                new RemoteFile("Datapack_1014274_2026-10-28.zip", 1_761_686_400L));

        // when
        Optional<String> latest = RemoteFile.latest(files, DATAPACK);

        // then
        assertEquals(Optional.of("Datapack_1014274_2026-10-29.zip"), latest);
    }

    @Test
    void findsNothingWhenNoFileMatches() {
        // given
        List<RemoteFile> files = List.of(new RemoteFile("readme.txt", 1_761_700_000L));

        // when
        Optional<String> latest = RemoteFile.latest(files, DATAPACK);

        // then
        assertEquals(Optional.empty(), latest);
    }
}
