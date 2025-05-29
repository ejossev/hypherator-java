package io.sevcik.hypherator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HyphenDictBuilder {
    private static final Logger logger = LoggerFactory.getLogger(HyphenDictBuilder.class);
    private static final List<String> DEFAULT_NO_HYPHEN_LIST = Arrays.stream("',–,’,-".split(",")).toList();


    public static HyphenDict fromFile(String filename) throws IOException {
        Path filePath = Paths.get(filename);

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return fromInputStream(inputStream);
        } catch (IOException e) {
            logger.error("Error reading file: " + filename, e);
            throw e;
        }
    }

    public static HyphenDict fromInputStream(InputStream inputStream) throws IOException {
        logger.info("Loading hyphenation dictionary from input stream");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        ) {
            HyphenDict dict = new HyphenDict();
            HyphenDict workingDict = dict;
            int level = 0;
            String line;
            line = reader.readLine();
            if (!"UTF8".equalsIgnoreCase(line) && !"UTF-8".equalsIgnoreCase(line)) {
                throw new RuntimeException("UTF-8 encoding expected");
            }

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("%") || line.startsWith("#") || line.trim().isEmpty())
                    continue;

                if (line.startsWith("LEFTHYPHENMIN")) {
                    workingDict.leftHyphenMin = Integer.parseInt(line.substring("LEFTHYPHENMIN".length()).trim());
                    continue;
                }
                if (line.startsWith("RIGHTHYPHENMIN")) {
                    workingDict.rightHyphenMin = Integer.parseInt(line.substring("RIGHTHYPHENMIN".length()).trim());
                    continue;
                }
                if (line.startsWith("COMPOUNDLEFTHYPHENMIN")) {
                    workingDict.leftCompoundMin = Integer.parseInt(line.substring("COMPOUNDLEFTHYPHENMIN".length()).trim());
                    continue;
                }
                if (line.startsWith("COMPOUNDRIGHTHYPHENMIN")) {
                    workingDict.rightCompoundMin = Integer.parseInt(line.substring("COMPOUNDRIGHTHYPHENMIN".length()).trim());
                    continue;
                }
                if (line.startsWith("NOHYPHEN")) {
                    // no hyphen rule `xyz` is equivalent to a rule `x10y10z10`
                    List<String> noHphenList = Arrays.stream(line.substring("NOHYPHEN".length()).trim().split(",")).toList();
                    for (String noHphen : noHphenList) {
                        workingDict.insertNoHyphen(noHphen);
                    }
                    continue;
                }
                if (line.startsWith("NEXTLEVEL")) {
                    level++;
                    workingDict.nextLevel = new HyphenDict();
                    workingDict = workingDict.nextLevel;
                    continue;
                }
                try {
                    addNormalRule(workingDict, line);
                } catch (RuntimeException e) {
                    logger.error("Error reading input rule for rule {}", line, e );
                }
            }

            if (level == 0) {
                HyphenDict baseLevel = new HyphenDict();
                baseLevel.leftHyphenMin = dict.leftHyphenMin;
                baseLevel.rightHyphenMin = dict.rightHyphenMin;
                baseLevel.leftCompoundMin = dict.leftCompoundMin > 0 ? dict.leftCompoundMin : dict.leftHyphenMin > 0 ? dict.leftHyphenMin : 3;
                baseLevel.rightCompoundMin = dict.rightCompoundMin > 0 ? dict.rightCompoundMin : dict.rightHyphenMin > 0 ? dict.rightHyphenMin : 3;
                // Add default values
                for (String noHphen : DEFAULT_NO_HYPHEN_LIST) {
                    baseLevel.insertNoHyphen(noHphen);
                }
                addNormalRule(baseLevel, "1-1");
                addNormalRule(baseLevel, "1'1");
                addNormalRule(baseLevel, "1–1");
                addNormalRule(baseLevel, "1’1");

                baseLevel.nextLevel = dict;
                dict = baseLevel;
            }
            return dict;
        } catch (IOException e) {
            logger.error("Error reading input stream", e);
            throw e;
        }
    }

    public static void addNormalRule(HyphenDict dict, String line) {
        logger.debug("Adding rule {}", line);
        if (".mas5száz8s3zok1ni/sz=3,1,1,14".equals(line)) {
            logger.info("Replacing broken rule: {}", line);
            line = ".mas5száz8s3zok1ni/sz=,3,1";
        }
        if ("gic5csetek/cs=,3,1 ,1,9".equals(line)) {
            logger.info("Replacing broken rule: {}", line);
            line = "gic5csetek/cs=,3,1";
        }
        HyphenDict.Rule rule = new HyphenDict.Rule();
        String replRule = null;
        if (line.contains("/")) {
            replRule = line.substring(line.indexOf("/") + 1);
            line = line.substring(0, line.indexOf("/"));
        }

        StringBuilder word = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            if (Character.isDigit(line.charAt(i))) {
                int value = Character.getNumericValue(line.charAt(i));
                rule.getBreakRules().put(word.length(), new HyphenDict.BreakRule().setValue(value));
            } else {
                word.append(line.charAt(i));
            }
        }

        // Set the match property of the rule
        rule.setMatch(word.toString());

        if (replRule != null) {
            String[] replData = replRule.split(",");
            if (replData.length == 3) {
                var replacement = replData[0];
                var replacementIndex = Integer.parseInt(replData[1]);
                var replacementCount = Integer.parseInt(replData[2]);

                HyphenDict.BreakRule relevantBreak = null;
                if (rule.getMatch().startsWith("."))
                    replacementIndex++;

                // now search if there is break within the given replacement region
                for (int i = replacementIndex-1; i < replacementIndex + replacementCount; i++) {
                    var potentialBreak = rule.getBreakRules().get(i);
                    if (potentialBreak == null)
                        continue;
                    if (relevantBreak != null)
                        throw new RuntimeException("CHECKPOINT - Multiple break rules within the same replacement region");
                    relevantBreak = potentialBreak;
                    replacementIndex = replacementIndex - i;
                }

                relevantBreak.setReplacement(replacement);
                relevantBreak.setReplacementIndex(replacementIndex);
                relevantBreak.setReplacementCount(replacementCount);
            } else if (replData.length == 1) {
                var replacement = replData[0];
                var replacementIndex = 1;
                var replacementCount = rule.getMatch().length();

                var relevantBreak = rule.getBreakRules().get(replacementIndex);
                relevantBreak.setReplacement(replacement);
                relevantBreak.setReplacementIndex(replacementIndex);
                relevantBreak.setReplacementCount(replacementCount);
            } else {
                throw new RuntimeException("CHECKPOINT - WE DO HANDLE replacements without indices and counts");
            }
        }

        dict.insertRule(rule);
    }


}
