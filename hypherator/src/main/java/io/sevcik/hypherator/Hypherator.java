package io.sevcik.hypherator;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.sevcik.hypherator.dto.DictionaryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Main entry point for working with the hyphenation package.
 * <p>
 * This class provides global, thread-safe access to all supported locale dictionaries.
 * Dictionaries are loaded once per classloader and always shared between all instances
 * of {@code Hyphenator}. You are free to create as many instances as you wish, as all
 * hyphenation data is managed and shared internally.
 * <br><br>
 * This approach ensures efficient memory usage and keeps
 * hyphenation operations lightweight for your application.
 * <p>
 * Use {@link #getInstance(String)} to create new hyphenation iterators for specific locales.
 * <p>
 * Sponsored by <a href="https://pdf365.cloud">pdf365.cloud</a>.
 */


public class Hypherator {
    private static final Logger logger = LoggerFactory.getLogger(Hypherator.class);
    private static final String ALL_JSON_PATH = "/hyphen/all.json";

    private static final Map<String, HyphenDict> dictionaries = new HashMap<>();
    static {
        try {
            Hypherator.loadDictionaries();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new Hyphenator instance and loads all dictionaries.
     * 
     * @throws IOException if there's an error loading the dictionaries
     */
    protected Hypherator() throws IOException {
        loadDictionaries();
    }

    /**
     * Retrieves a new {@link HyphenationIterator} instance for the given locale.
     *
     * @param locale the locale identifier (e.g. "en-US")
     * @return a new {@link HyphenationIterator} for the locale, or {@code null} if no dictionary is available for the locale
     *
     * <p>
     * <b>Usage Note:</b> The returned iterator is the recommended way to access hyphenation points and process hyphenation in text.
     * Calling this method repeatedly for the same locale will create a new iterator instance each time,
     * but the underlying dictionary is not shared between {@code Hyphenator} instances. For efficiency,
     * load and reuse the {@code Hyphenator} and its dictionaries as a singleton.
     * </p>
     */
    public static HyphenationIterator getInstance(String locale) {
        locale = locale.replace('_', '-');
        HyphenDict dict = dictionaries.get(locale);
        if (dict == null) {
            return null;
        }
        return new HyphenationIteratorImpl(dict);
    }

    /**
     * Builds a new {@link HyphenationIterator} instance from provided input stream
     * @param inputStream the input stream with dictionary data
     * @return a new {@link HyphenationIterator} for the given input stream
     * @throws IOException In case the dictionary cannot be read.
     */
    public static HyphenationIterator getInstance(InputStream inputStream) throws IOException{
        HyphenDict dict = HyphenDictBuilder.fromInputStream(inputStream);
        return new HyphenationIteratorImpl(dict);
    }

    /**
     * Loads all dictionaries from the all.json resource file.
     * 
     * @throws IOException if there's an error loading the dictionaries
     */
    protected static void loadDictionaries() throws IOException {
        try (InputStream is = Hypherator.class.getResourceAsStream(ALL_JSON_PATH)) {
            if (is == null) {
                throw new IOException("Resource not found: " + ALL_JSON_PATH);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            List<DictionaryEntry> entries = objectMapper.readValue(is, new TypeReference<List<DictionaryEntry>>() {});

            int dictionaryCount = 0;
            int localeCount = 0;

            for (DictionaryEntry entry : entries) {
                if (entry.getLocations() == null || entry.getLocations().isEmpty() || entry.getLocales() == null || entry.getLocales().isEmpty()) {
                    continue;
                }

                String location = entry.getLocations().get(0);
                String resourcePath = "/hyphen/" + location;

                // Load the dictionary using HyphenDictBuilder
                try {
                    logger.info("Loading dictionary: {} {}", resourcePath, entry.getLocales());
                    HyphenDict dict = loadDictionaryFromResource(resourcePath);
                    dictionaryCount++;

                    // Add dictionary for each locale
                    for (String locale : entry.getLocales()) {
                        dictionaries.put(locale, dict);
                        localeCount++;
                    }
                } catch (IOException e) {
                    logger.warn("Failed to load dictionary: {}", resourcePath, e);
                }
            }

            logger.info("Loaded {} dictionaries for {} locales", dictionaryCount, localeCount);
        }
    }

    /**
     * Loads a dictionary from a resource path.
     * 
     * @param resourcePath the path to the dictionary resource
     * @return the loaded dictionary
     * @throws IOException if there's an error loading the dictionary
     */
    protected static HyphenDict loadDictionaryFromResource(String resourcePath) throws IOException {
        try (InputStream is = Hypherator.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            // Load the dictionary directly from the input stream
            return HyphenDictBuilder.fromInputStream(is);
        }
    }

    protected Map<String, HyphenDict> getDictionaries() {
        return dictionaries;
    }

    protected HyphenDict getDictionary(String locale) {
        return dictionaries.get(locale);
    }

}
