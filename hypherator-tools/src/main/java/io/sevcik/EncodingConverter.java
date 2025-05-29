package io.sevcik;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class EncodingConverter {

    public static void convertToUTF8(String fileName) throws IOException {
        Path filePath = Paths.get(fileName);

        // Read raw bytes for the first line to extract encoding safely
        String encodingLine;
        try (InputStream inputStream = Files.newInputStream(filePath);
             BufferedReader rawReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {

            encodingLine = rawReader.readLine();
            if (encodingLine == null) {
                throw new IOException("File is empty or encoding line missing");
            }
        }


        if (encodingLine == null) {
            throw new IOException("File is empty or encoding line missing");
        }

        String detectedEncoding = encodingLine.trim();

        if (detectedEncoding.equalsIgnoreCase("UTF8") || detectedEncoding.equalsIgnoreCase("UTF-8")) {
            System.out.println("File is already in UTF-8 encoding.");
            return;
        }

        // Read the entire file using detected encoding
        Charset sourceCharset = Charset.forName(detectedEncoding);
        BufferedReader reader = Files.newBufferedReader(filePath, sourceCharset);
        StringBuilder contentBuilder = new StringBuilder();

        // Skip encoding line
        reader.readLine();

        String line;
        while ((line = reader.readLine()) != null) {
            contentBuilder.append(line).append(System.lineSeparator());
        }
        reader.close();

        // Write back in UTF-8 encoding (overwrite original file)
        BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
        writer.write("UTF-8" + System.lineSeparator());
        writer.write(contentBuilder.toString());
        writer.close();

        System.out.println("File converted to UTF-8.");
    }

}