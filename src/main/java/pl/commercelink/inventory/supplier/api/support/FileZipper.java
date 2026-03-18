package pl.commercelink.inventory.supplier.api.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileZipper {

    public static Map<String, byte[]> gunzip(byte[] bytes) {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return Map.of("file", outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, byte[]> unzip(byte[] bytes) {
        Map<String, byte[]> unzippedFiles = new HashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[4096];
            ZipEntry entry = zipInputStream.getNextEntry();

            while (entry != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    int bytesRead;
                    while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    unzippedFiles.put(entry.getName(), outputStream.toByteArray());
                }
                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return unzippedFiles;
    }
}
