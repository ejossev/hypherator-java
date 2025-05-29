package io.sevcik.hypherator;

import java.util.*;

 class HyphenDict {
    protected Integer leftHyphenMin = 0;
    protected Integer rightHyphenMin = 0;
    protected Integer leftCompoundMin = 0;
    protected Integer rightCompoundMin = 0;

    protected Map<String, Rule> rules = new HashMap<String, Rule>();
    protected HyphenDict nextLevel = null;
    protected List<String> noHyphens = new ArrayList<>();


    protected void insertRule(Rule newRule) {
        var key = newRule.match;
        if (rules.containsKey(key)) {
            var existingRule = rules.get(key);
            for (var newBreakRule : newRule.breakRules.entrySet()) {
                var existingBreakRule = existingRule.getBreakRules().get(newBreakRule.getKey());
                if (existingBreakRule != null) {
                    if (!existingBreakRule.equals(newBreakRule.getValue())) {
                        // in such a case, which one has higher priority?
                        var oldPriority = existingBreakRule.getValue();
                        var newPriority = newBreakRule.getValue().getValue();
                        if (newPriority > oldPriority) {
                            existingRule.breakRules.put(newBreakRule.getKey(), newBreakRule.getValue());
                        }
                    }
                } else {
                    existingRule.breakRules.put(newBreakRule.getKey(), newBreakRule.getValue());
                }
            }
        } else {
            rules.put(key, newRule);
        }
    }

    protected void insertNoHyphen(String noHyphen) {
        noHyphens.add(noHyphen);
    }

    public static class Rule {
        String match;
        Map<Integer, BreakRule> breakRules = new HashMap<Integer, BreakRule>();

        public String getMatch() {
            return match;
        }

        public Rule setMatch(String match) {
            this.match = match;
            return this;
        }

        public Map<Integer, BreakRule> getBreakRules() {
            return breakRules;
        }
    }

    public static class BreakRule {
        int value;
        String replacement = null;
        int replacementIndex = 0;
        int replacementCount = 0;

        public int getValue() {
            return value;
        }

        public BreakRule setValue(int value) {
            this.value = value;
            return this;
        }

        public String getReplacement() {
            return replacement;
        }

        public BreakRule setReplacement(String replacement) {
            this.replacement = replacement;
            return this;
        }

        public int getReplacementIndex() {
            return replacementIndex;
        }

        public BreakRule setReplacementIndex(int replacementIndex) {
            this.replacementIndex = replacementIndex;
            return this;
        }

        public int getReplacementCount() {
            return replacementCount;
        }

        public BreakRule setReplacementCount(int replCount) {
            this.replacementCount = replCount;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BreakRule breakRule)) return false;
            return value == breakRule.value && replacementIndex == breakRule.replacementIndex && replacementCount == breakRule.replacementCount && Objects.equals(replacement, breakRule.replacement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, replacement, replacementIndex, replacementCount);
        }
    }

}


