package io.sevcik.hypherator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sevcik.hypherator.dto.PotentialBreak;
import org.junit.jupiter.api.Test;

import static io.sevcik.hypherator.HyphenationIterator.DONE;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HypheratorTest {
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testHyphenatorLoadsAllDictionaries() throws IOException {
        // Create a new Hyphenator instance
        Hypherator hypherator = new Hypherator();

        // Get all dictionaries
        Map<String, HyphenDict> dictionaries = hypherator.getDictionaries();
        
        // Verify that dictionaries were loaded
        assertFalse(dictionaries.isEmpty(), "Dictionaries should not be empty");
        
        // Test a few specific locales
        assertNotNull(hypherator.getDictionary("en-US"), "English (US) dictionary should be loaded");
        assertNotNull(hypherator.getDictionary("de-DE"), "German dictionary should be loaded");
        assertNotNull(hypherator.getDictionary("fr-FR"), "French dictionary should be loaded");
        
        // Test using Locale object
        assertNotNull(hypherator.getDictionary(Locale.US.toString().replace("_", "-")), "English (US) dictionary should be loaded using Locale");
        
        // Print the number of dictionaries loaded
        System.out.println("Loaded " + dictionaries.size() + " dictionaries");
    }

    @Test
    public void testHyphenSign() throws IOException {
        // Create a new Hyphenator instance
        Hypherator hypherator = new Hypherator();
        var iterator = Hypherator.getInstance("de");
        assertEquals("-", iterator.getHyphen());

        iterator = Hypherator.getInstance("ta-IN");
        assertEquals("", iterator.getHyphen());
    }

    @Test
    public void testRealWorldIssues() throws IOException {
        // Create a new Hyphenator instance
        Hypherator hypherator = new Hypherator();

        var iterator = Hypherator.getInstance("de");
        iterator.setWord("mitgeteilt");
        iterator.setUrgency(9);

        var pb = iterator.first();
        int count = 0;
        while (pb != DONE) {
            var parts = iterator.applyBreak(pb);
            System.out.println(parts.getFirst() + " - " + parts.getSecond());
            pb = iterator.next();
            count++;
        }

        assertEquals(2, count);
    }


    @Test
    public void testHyphenation() throws IOException {
        List<String> allTcs = List.of();
        try (InputStream tcStream = getClass().getResourceAsStream("/data/testcases.txt")) {
            allTcs = new java.io.BufferedReader(new java.io.InputStreamReader(tcStream))
                    .lines()
                    .map(String::trim)
                    .toList();
        } catch (IOException e) {
            System.out.println("No test cases found");
        }

        for (String tcName : allTcs) {
            try (InputStream dictStream = getClass().getResourceAsStream("/data/" + tcName + ".dic");
                 InputStream dataStream = getClass().getResourceAsStream("/data/" + tcName + ".dat")) {

                assertNotNull(dictStream, "Dictionary not found for test case: " + tcName);
                assertNotNull(dataStream, "Data not found for test case: " + tcName);

                HyphenDict dict = HyphenDictBuilder.fromInputStream(dictStream);

                // Read all lines, trim, keep blank lines for splitting
                List<String> allLines = new java.io.BufferedReader(new java.io.InputStreamReader(dataStream))
                        .lines()
                        .map(String::trim)
                        .toList();

                // Split into blocks separated by empty lines (blank lines)
                List<List<String>> blocks = new java.util.ArrayList<>();
                List<String> current = new java.util.ArrayList<>();

                for (String line : allLines) {
                    if (line.isEmpty()) {
                        if (!current.isEmpty()) {
                            blocks.add(List.copyOf(current));
                            current.clear();
                        }
                    } else {
                        current.add(line);
                    }
                }
                if (!current.isEmpty()) {
                    blocks.add(current); // Add the last block if present
                }

                assertFalse(blocks.isEmpty(), "No cases found in data file: " + tcName);

                int caseIdx = 1;
                Hyphenate hypernate = new HyphenateImpl();
                for (List<String> block : blocks) {
                    assertFalse(block.isEmpty(), "Empty test block in " + tcName);
                    String word = block.get(0);
                    List<String> expectedHyphens = block.subList(1, block.size());

                    List<PotentialBreak> breaks = hypernate.hyphenate(dict, word);
                    List<String> produced = breaks.stream()
                            .map(breakRule -> {
                                var broken = hypernate.applyBreak(word, breakRule);
                                return broken.getFirst() + "=" + broken.getSecond();
                            })
                            .toList();

                    System.out.println("Test case: " + tcName + " #" + caseIdx);
                    System.out.println("Input: " + word);
                    System.out.println("Breaks: " + mapper.writeValueAsString(breaks));
                    System.out.println("Expected: " + expectedHyphens);
                    System.out.println("Produced: " + produced);

                    assertEquals(expectedHyphens.size(), produced.size());
                    for (String exp : expectedHyphens) {
                        assertTrue(produced.contains(exp),
                            "Expected hyphenation not found for '" + word + "' in " + tcName + ": " + exp);
                    }
                    caseIdx++;
                }
            }
        }
    }


}