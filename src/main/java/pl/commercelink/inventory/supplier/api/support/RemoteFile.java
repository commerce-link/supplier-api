package pl.commercelink.inventory.supplier.api.support;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;

record RemoteFile(String name, long modifiedAt) {

    static Optional<String> latest(Collection<RemoteFile> files, Pattern fileName) {
        return files.stream()
                .filter(file -> fileName.matcher(file.name()).matches())
                .max(Comparator.comparingLong(RemoteFile::modifiedAt).thenComparing(RemoteFile::name))
                .map(RemoteFile::name);
    }
}
