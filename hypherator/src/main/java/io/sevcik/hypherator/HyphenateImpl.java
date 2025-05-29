package io.sevcik.hypherator;

import io.sevcik.hypherator.dto.Pair;
import io.sevcik.hypherator.dto.PotentialBreak;

import java.util.ArrayList;
import java.util.List;

class HyphenateImpl implements Hyphenate {
    @Override
    public Pair<String, String> applyBreak(String text, PotentialBreak breakRule) {
        PotentialBreakImpl breakRuleImpl = (PotentialBreakImpl) breakRule;
        if (breakRuleImpl.breakRule().replacement != null) {
            // aaa5bbb/ab=ab,3,2
            var replacementStart = breakRuleImpl.position() + breakRuleImpl.breakRule().replacementIndex - 1; // because replacement index by spec starts at 1
            var substituedText =
                    text.substring(0, replacementStart) + // position points AFTER the character in question, so we have to cut one before
                            breakRuleImpl.breakRule().replacement +
                            text.substring(replacementStart + breakRuleImpl.breakRule().replacementCount);

            return new Pair<>(
                    substituedText.substring(0, substituedText.indexOf("=")),
                    substituedText.substring(substituedText.indexOf("=") + 1)
            );
        } else {
            return new Pair<>(text.substring(0, breakRuleImpl.position()), text.substring(breakRuleImpl.position()));
        }
    }

    @Override
    public List<PotentialBreak> hyphenate(HyphenDict dict, String text) {
        List<PotentialBreak> possibleBreaks = applyStandardRules(dict, text, true, true);
        applyNohyphenRules(dict, text, possibleBreaks);
        // apply rules for not breaking too close to the word ends
        int digitsFromLeft = 0;
        int digitsFromRight = 0;
        while (digitsFromLeft < text.length() && text.charAt(digitsFromLeft) >= '0' && text.charAt(digitsFromLeft) <= '9')
            digitsFromLeft++;
        while (digitsFromRight < text.length() && text.charAt(text.length() - 1 - digitsFromRight) >= '0' && text.charAt(text.length() - 1 - digitsFromRight) <= '9')
            digitsFromRight++;
        int finalDigitsFromLeft = digitsFromLeft;
        int finalDigitsFromRight = digitsFromRight;
        possibleBreaks.removeIf(
                breakRule -> ((PotentialBreakImpl)breakRule).position() < dict.leftHyphenMin + finalDigitsFromLeft ||
                        ((PotentialBreakImpl)breakRule).position() > text.length() - dict.rightHyphenMin - finalDigitsFromRight);
        return possibleBreaks;
    }

    private void applyNohyphenRules(HyphenDict dict, String text, List<PotentialBreak> possibleBreaks) {
        for (String noHyphen : dict.noHyphens) {
            int index = text.indexOf(noHyphen);
            while (index >= 0) {
                final int start = index;
                final int end = index + noHyphen.length();
                possibleBreaks.removeIf(pb -> ((PotentialBreakImpl)pb).position() == start ||  ((PotentialBreakImpl)pb).position() == end);
                index = text.indexOf(noHyphen, index + 1);
            }
        }
    }



    private List<PotentialBreak> applyStandardRules(HyphenDict dict, String text, boolean isWordLeftEnd, boolean isWordRightEnd) {
        text = "." + text.replaceAll("\\d", ".") + ".";
        Pair<Integer, HyphenDict.BreakRule>[] potentialBreaks = new Pair[text.length()];
        for (int i = 0; i < potentialBreaks.length; i++) {
            potentialBreaks[i] = new Pair<>(0, null);
        }

        applyRulesFromDict(dict, text, potentialBreaks);

        if (dict.nextLevel != null) {
            int lastBreakPosition = 1; // why 1 / -1? because of the dots added at the beginnign and end of the word
            for (int i = 2; i <= potentialBreaks.length - 1; i++) {
                if (i == potentialBreaks.length - 1 && lastBreakPosition == 1) {
                    // We cannot further split this word using compound rules - the word is not compount anymore, apply nextlevel rules
                    var newBreaks = applyStandardRules(dict.nextLevel, text.substring(1, text.length() - 1), isWordLeftEnd, isWordRightEnd);
                    mergeBreakList(newBreaks, lastBreakPosition, potentialBreaks);
                    applyBorderRules(potentialBreaks, dict.leftCompoundMin, dict.rightCompoundMin, isWordLeftEnd, isWordRightEnd);
                } else if (((potentialBreaks[i].getFirst() % 2 == 1) || (i == potentialBreaks.length - 1))) {
                    // This word was broken down, so try to apply compound rules to subparts
                    var potentialBreak = potentialBreaks[i].getSecond();
                    var previousBreak = potentialBreaks[lastBreakPosition].getSecond();
                    String segment = text.substring(lastBreakPosition, i);
                    segment = applyReplacementToSegment(segment, previousBreak, potentialBreak);

                    int segmentOffsetAfterReplacement = 0;
                    if (previousBreak != null && previousBreak.replacement != null) {
                        String replacementRight = previousBreak.replacement.substring(previousBreak.replacement.indexOf("=") + 1);
                        segmentOffsetAfterReplacement =
                                (previousBreak.replacementIndex + previousBreak.replacementCount - 1) - replacementRight.length();
                    }

                    var newBreaks = applyStandardRules(dict, segment, i == 1 && isWordLeftEnd, i == potentialBreaks.length - 1 && isWordRightEnd);
                    mergeBreakList(newBreaks, lastBreakPosition + segmentOffsetAfterReplacement, potentialBreaks);
                    lastBreakPosition = i;
                }
            }
        }

        List<PotentialBreak> result = new ArrayList<>();
        for (int i = 1; i < potentialBreaks.length; i++) {
            if (potentialBreaks[i].getFirst() % 2 == 1) {
                result.add(new PotentialBreakImpl(i-1, potentialBreaks[i].getFirst(), potentialBreaks[i].getSecond()));
            }
        }
        return result;
    }

    private String applyReplacementToSegment(String segment, HyphenDict.BreakRule breakLeft, HyphenDict.BreakRule breakRight) {
        if (breakLeft != null && breakLeft.replacement != null) {
            segment = breakLeft.replacement.substring(breakLeft.replacement.indexOf("=") + 1) +
                    segment.substring(breakLeft.replacementIndex + breakLeft.replacementCount - 1);
        }
        if (breakRight != null && breakRight.replacement != null) {
            var replacementStart = segment.length() - 1 + breakRight.replacementIndex;
            segment = segment.substring(0, replacementStart) + breakRight.replacement.substring(0, breakRight.replacement.indexOf("="));
        }
        return segment;
    }

    private void mergeBreakList(List<PotentialBreak> newBreaks, int offset, Pair<Integer, HyphenDict.BreakRule>[] potentialBreaks) {
        for (var newBreak : newBreaks) {
            var newBreakPosition = ((PotentialBreakImpl)newBreak).position() + offset;
            potentialBreaks[newBreakPosition].setFirst(((PotentialBreakImpl)newBreak).priority());
            potentialBreaks[newBreakPosition].setSecond(((PotentialBreakImpl)newBreak).breakRule());
        }
    }

    private void applyRulesFromDict(HyphenDict dict, String text, Pair<Integer, HyphenDict.BreakRule>[] breakCandidates) {
        int textLength = text.length();
        for (int start = 0; start < textLength - 1; start++) {
            for (int end = start + 1; end <= textLength; end++) {
                String toMatch = text.substring(start, end);
                if (dict.rules.containsKey(toMatch)) {
                    for (var breakRuleEntry : dict.rules.get(toMatch).getBreakRules().entrySet()) {
                        int breakPosition = start + breakRuleEntry.getKey();
                        int priority = breakRuleEntry.getValue().getValue();
                        if (priority > breakCandidates[breakPosition].getFirst()) {
                            breakCandidates[breakPosition].setFirst(priority);
                            breakCandidates[breakPosition].setSecond(breakRuleEntry.getValue());
                        }
                    }
                }
            }
        }
    }


    private void applyBorderRules(Pair<Integer, HyphenDict.BreakRule>[] potentialBreaks, int leftHyphenMin, int rightHyphenMin, boolean isWordLeftEnd, boolean isWordRightEnd) {
        if (!isWordLeftEnd) {
            for (int i = 0; i <= leftHyphenMin; i++) {
                potentialBreaks[i].setFirst(0);
                potentialBreaks[i].setSecond(null);
            }
        }

        if (!isWordRightEnd) {
            for (int i = potentialBreaks.length - rightHyphenMin; i < potentialBreaks.length; i++) {
                potentialBreaks[i].setFirst(0);
                potentialBreaks[i].setSecond(null);
            }
        }
    }


}
