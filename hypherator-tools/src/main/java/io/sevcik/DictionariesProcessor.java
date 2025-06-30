package io.sevcik;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class DictionariesProcessor {
    private final String licenseDirectory;
    private final String hyphenDirectory;
    private static final List<String> nonCompatibleLocales = Arrays.asList("cs-CZ", "pt-PT", "eo", "ro-RO");

    private static boolean isCompatibleLocale(List<String> locales) {
        boolean compatible = true;
        for (String s : nonCompatibleLocales) {
            if (locales.contains(s)) {
                compatible = false;
                break;
            }
        }
        return compatible;
    }

    static class HyphenData {
        List<String> locations;
        List<String> locales;
        String hyphen = "-";

        public HyphenData(List<String> locations, List<String> locales) {
            this.locations = locations;
            this.locales = locales;
        }

        public List<String> getLocations() {
            return locations;
        }

        public HyphenData setLocations(List<String> locations) {
            this.locations = locations;
            return this;
        }

        public List<String> getLocales() {
            return locales;
        }

        public HyphenData setLocales(List<String> locales) {
            this.locales = locales;
            return this;
        }

        public HyphenData setHyphen(String hyphen) {
            this.hyphen = hyphen;
            return this;
        }

        public String getHyphen() {
            return hyphen;
        }
    }

    private final List<HyphenData> hyphenData = new ArrayList<>();

    public DictionariesProcessor(String licenseDirectory, String hyphenDirectory) {
        this.licenseDirectory = licenseDirectory;
        this.hyphenDirectory = hyphenDirectory;
    }


    void processDirectoryLibreOffice(String directory) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            System.err.println("Error: " + directory + " is not a directory");
            return;
        }

        File[] subdirectories = dir.listFiles(File::isDirectory);
        if (subdirectories == null) {
            System.err.println("Error: Could not list subdirectories in " + directory);
            return;
        }

        for (File subdir : subdirectories) {
            System.out.println("Processing directory: " + subdir.getPath());
            File dictionaryFile = new File(subdir, "dictionaries.xcu");
            if (dictionaryFile.exists() && dictionaryFile.isFile()) {
                processDictionaryDefinition(dictionaryFile, subdir.getPath());
            }
        }
    }

    void processDirectoryLocaleSubdirs(String directory) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            System.err.println("Error: " + directory + " is not a directory");
            return;
        }

        File[] subdirectories = dir.listFiles(File::isDirectory);
        if (subdirectories == null) {
            System.err.println("Error: Could not list subdirectories in " + directory);
            return;
        }

        for (File subdir : subdirectories) {
            if (!subdir.getName().contains("_"))
                continue;
            System.out.println("Processing directory: " + subdir.getPath());
            String localeName = subdir.getName();
            String languageTag = localeName.substring(0, localeName.indexOf("_"));
            List<String> locales = List.of(localeName.replace("_", "-"), languageTag);

            var licenseFiles = subdir.listFiles((dir1, name) -> name.contains("license") || name.contains("LICENSE"));
            File licenseFile = null;
            if (licenseFiles != null && licenseFiles.length > 0)
                licenseFile = licenseFiles[0];

            var dictFile = subdir.listFiles((dir1, name) -> name.endsWith(".dic"));
            File patternFile = null;
            if (dictFile != null && dictFile.length > 0)
                patternFile = dictFile[0];

            System.out.println("About to copy");
            if (patternFile != null && licenseFile != null) {
                Path licenseDir = Path.of(licenseDirectory, localeName);
                Path hyphenDir = Path.of(hyphenDirectory, localeName);
                createOrDeleteDirectory(licenseDir);
                createOrDeleteDirectory(hyphenDir);
                Path targetLic = licenseDir.resolve(licenseFile.getName());
                Path targetDic = hyphenDir.resolve(patternFile.getName());
                try {
                    System.out.println("Copying license file: " + licenseFile.getPath());
                    Files.copy(licenseFile.toPath(), targetLic, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Copying pattern file: " + patternFile.getPath());
                    Files.copy(patternFile.toPath(), targetDic, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.err.println(e);
                    e.printStackTrace();
                    System.err.println("Error copying hyphenation files");
                }
                var existingEntries = hyphenData.stream().filter(d -> d.locales.contains(languageTag) || d.locales.contains(localeName.replace("_", "-"))).toList();
                hyphenData.removeAll(existingEntries);
                hyphenData.add(new HyphenData(List.of(localeName + "/" + patternFile.getName()), locales).setHyphen(""));
            }
        }
    }

    void processDictionaryDefinition(File dictionaryFile, String basePath) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(dictionaryFile);
            doc.getDocumentElement().normalize();
            boolean alreadyProcessedDirData = false;

            NodeList serviceManagerNodes = doc.getElementsByTagName("node");
            for (int i = 0; i < serviceManagerNodes.getLength(); i++) {
                Element serviceManager = (Element) serviceManagerNodes.item(i);
                if ("ServiceManager".equals(serviceManager.getAttribute("oor:name"))) {
                    NodeList dictionariesNodes = serviceManager.getElementsByTagName("node");

                    for (int j = 0; j < dictionariesNodes.getLength(); j++) {
                        Element dictionaries = (Element) dictionariesNodes.item(j);
                        if ("Dictionaries".equals(dictionaries.getAttribute("oor:name"))) {
                            // Process dictionary entries
                            NodeList dictionaryEntries = dictionaries.getChildNodes();
                            for (int k = 0; k < dictionaryEntries.getLength(); k++) {
                                Node entry = dictionaryEntries.item(k);
                                if (entry.getNodeType() == Node.ELEMENT_NODE && "node".equals(entry.getNodeName())) {
                                    Element dictionaryEntry = (Element) entry;
                                    alreadyProcessedDirData = processHyphenationDictionary(dictionaryEntry, basePath, alreadyProcessedDirData);
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }

        } catch (ParserConfigurationException e) {
            System.err.println("Error creating XML parser: " + e.getMessage());
        } catch (SAXException e) {
            System.err.println("Error parsing XML file " + dictionaryFile.getName() + ": " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading file " + dictionaryFile.getName() + ": " + e.getMessage());
        }

    }

    private boolean processHyphenationDictionary(Element dictionaryEntry, String basePath, boolean alreadyProcessedDirData) {
        NodeList props = dictionaryEntry.getElementsByTagName("prop");
        boolean isHyphen = false;
        String locations = null;
        String locales = null;

        for (int i = 0; i < props.getLength(); i++) {
            Element prop = (Element) props.item(i);
            String propName = prop.getAttribute("oor:name");
            NodeList values = prop.getElementsByTagName("value");

            if (values.getLength() > 0) {
                String value = values.item(0).getTextContent();

                if ("Format".equals(propName)) {
                    isHyphen = "DICT_HYPH".equals(value);
                } else if ("Locations".equals(propName)) {
                    locations = value;
                } else if ("Locales".equals(propName)) {
                    locales = value;
                }
            }
        }

        if (isHyphen && locations != null && locales != null) {
            processHyphenData(locations, locales, basePath, alreadyProcessedDirData);
            return true;
        } else if (isHyphen) {
            System.err.println("Warning: Skipping hyphenation dictionary without locations or locales: " + dictionaryEntry.getAttribute("oor:name"));
        }
        return alreadyProcessedDirData;
    }


    void processHyphenData(String locations, String locales, String basePath, boolean alreadyProcessedDirData) {
        List<String> locationFiles = Arrays.asList(locations.replace("%origin%", basePath).split(" "));
        List<String> localesList = Arrays.asList(locales.split(" "));

        if (!isCompatibleLocale(localesList)) {
            return;
        }
        String directoryName = new File(basePath).getName();

        System.out.println("Found hyphen dictionary:");
        System.out.println("Locations: " + locationFiles);
        System.out.println("Locales: " + localesList);
        System.out.println("Is new directory: " + !alreadyProcessedDirData);

        Path licenseDir = Path.of(licenseDirectory, directoryName);
        Path hyphenDir = Path.of(hyphenDirectory, directoryName);

        if (!alreadyProcessedDirData) {
            // Create and clean the directories
            createOrDeleteDirectory(licenseDir);
            createOrDeleteDirectory(hyphenDir);
        }

        // Copy hyphenation files to hyphen directory
        for (String locationFile : locationFiles) {
            Path source = Path.of(locationFile);
            if (locales.startsWith("ca-")) // Hack, catalan files are in a subdirectory
                source = Path.of(source.getParent().toString(), "dictionaries", source.getFileName().toString());
            Path target = hyphenDir.resolve(source.getFileName());
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                EncodingConverter.convertToUTF8(target.toString());
            } catch (IOException e) {
                System.err.println(e);
                e.printStackTrace();
                System.err.println("Error copying hyphenation file: " + source + " to " + target + ": " + e.getMessage());
            }
        }

        // Copy license and documentation files
        try {
            Path sourceDir = Path.of(basePath);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
                for (Path path : stream) {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".txt") ||
                            fileName.toUpperCase().startsWith("LICENSE") ||
                            fileName.toUpperCase().startsWith("LICENCE") ||
                            fileName.startsWith("README")) {

                        Path target = licenseDir.resolve(path.getFileName());
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error copying license/documentation files: " + e.getMessage());
        }

        List<String> relativeLocations = locationFiles.stream()
                .map(loc -> directoryName + "/" + new File(loc).getName())
                .toList();

        hyphenData.add(new HyphenData(relativeLocations, localesList));
    }

    void createOrDeleteDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Error deleting: " + path + ": " + e.getMessage());
                            }
                        });
            }
            Files.createDirectories(directory);
        } catch (IOException e) {
            System.err.println("Error managing directory: " + e.getMessage());
        }

    }

    private void extractDefaultLocales() {
        Set<String> knownLocalesOfLanguageCodeOnly = new HashSet<>();

        // First, figure out which files have defined standalone languege code
        for (HyphenData data : hyphenData) {
            for (String locale : data.locales) {
                if (!locale.contains("-")) {
                    knownLocalesOfLanguageCodeOnly.add(locale);
                }
            }
        }

        // Now walk through the dictionaries files and set the standalone language code if not defined for that group
        for (HyphenData data : hyphenData) {
            List<String> newLocales = new ArrayList<>(data.locales);

            for (String locale : data.locales) {
                if (locale.contains("-")) {
                    String languageCode = locale.split("-")[0];
                    if (knownLocalesOfLanguageCodeOnly.contains(languageCode))
                        continue;

                    knownLocalesOfLanguageCodeOnly.add(languageCode);
                    newLocales.add(languageCode);

                    // use slovak dictionary for Czech language due to licensing reasons:
                    if ("sk".equals(languageCode)) {
                        newLocales.add("cs");
                        knownLocalesOfLanguageCodeOnly.add("cs");
                    }
                }
            }

            data.locales = newLocales;
        }
    }

    private void addManualDictionaries() {
        hyphenData.add(new HyphenData(List.of("ro/ro.dic"), List.of("ro", "ro-RO")));
        hyphenData.add(new HyphenData(List.of("la/hyph_la.dic"), List.of("la")));
    }

    private void saveHyphenDataToJson() {
        extractDefaultLocales();
        addManualDictionaries();
        hyphenData.sort(Comparator.comparing(a -> a.locations.get(0)));
        Path jsonFile = Path.of(hyphenDirectory, "all.json");
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(jsonFile.toFile(), hyphenData);
            System.out.println("Saved hyphenation data to: " + jsonFile);
        } catch (IOException e) {
            System.err.println("Error saving hyphenation data to JSON: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        System.out.println("Hypherator Dictionaries Processor");
        System.out.println("Current path: " + System.getProperty("user.dir"));
        if (args.length != 3) {
            args = List.of("dictionaries", "indic_dictionaries", "hypherator/src/main/resources/3pp_licenses", "hypherator/src/main/resources/hyphen").toArray(new String[0]);
            //System.err.println("Usage: DictionariesProcessor <sourceDirectory> <licenseDirectory> <hyphenDirectory>");
            //System.exit(1);
        }

        String loSourceDirectory = args[0];
        String byLocaleDirectory = args[1];
        String licenseDirectory = args[2];
        String hyphenDirectory = args[3];

        // Create directories if they don't exist
        try {
            Files.createDirectories(Path.of(licenseDirectory));
            Files.createDirectories(Path.of(hyphenDirectory));
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
            System.exit(1);
        }

        DictionariesProcessor processor = new DictionariesProcessor(licenseDirectory, hyphenDirectory);
        processor.processDirectoryLibreOffice(loSourceDirectory);
        processor.processDirectoryLocaleSubdirs(byLocaleDirectory);
        processor.saveHyphenDataToJson();

    }
}
