package pl.commercelink.inventory.supplier.api.support;

import java.io.*;

public class ProductFeedPurifier {

    public byte[] purify(byte[] inputData) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(inputData);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String newLine = line.replace("\"", "");
                writer.write(newLine);
                writer.newLine();
            }
            writer.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to purify raw feed data", e);
        }
    }

}
