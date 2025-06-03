package io.sevcik.hypherator;

import io.sevcik.hypherator.dto.Pair;
import io.sevcik.hypherator.dto.PotentialBreak;

import java.util.List;

public class HyphenationIteratorImpl implements HyphenationIterator {
    private String word = null;
    private int priorityFilter = 1;
    private final HyphenDict dict;
    private final Hyphenate hyphenate;
    private List<PotentialBreak> breaks = null;
    private int index = -1; // -1 indicates not initialized

    protected HyphenationIteratorImpl(HyphenDict dict) {
        this.dict = dict;
        this.hyphenate = new HyphenateImpl();
    }
    
    @Override
    public void setUrgency(int urgency) {
        this.priorityFilter = 10 - urgency;
        resetState();
    }
    
    @Override
    public void setWord(String word) {
        this.word = word;
        this.breaks = hyphenate.hyphenate(dict, word);
        resetState();
    }

    @Override
    public PotentialBreak first() {
        if (word == null) throw new IllegalStateException("No word has been set");
        index = 0;
        while (index < breaks.size() && ((PotentialBreakImpl)breaks.get(index)).priority() < priorityFilter) index++;
        if (index < breaks.size()) {
            return breaks.get(index);
        }
        return HyphenationIterator.DONE;
    }

    @Override
    public PotentialBreak next() {
        if (index == -1) throw new IllegalStateException("Iterator not initialized with first()");
        if (word == null) throw new IllegalStateException("No word has been set");
        index++;
        while (index < breaks.size() && ((PotentialBreakImpl)breaks.get(index)).priority() < priorityFilter) index++;
        if (index < breaks.size()) {
            return breaks.get(index);
        }
        return HyphenationIterator.DONE;
    }
    
    @Override
    public Pair<String, String> applyBreak(PotentialBreak breakRule) {
        return hyphenate.applyBreak(word, breakRule);
    }

    @Override
    public HyphenationIterator getInstanceOnRightPart(PotentialBreak breakRule) {
        if (breaks == null) throw new IllegalStateException("No word has been set");
        if (breakRule == null) throw new IllegalArgumentException("Break rule cannot be null");
        var parts = hyphenate.applyBreak(word, breakRule);
        var newPotentialBreaksList = hyphenate.getFurtherHyphenations(dict, breaks, breakRule, parts.getSecond());
        var newIterator = new HyphenationIteratorImpl(dict);

        newIterator.priorityFilter = priorityFilter;
        newIterator.word = parts.getSecond();
        newIterator.breaks = newPotentialBreaksList;
        newIterator.index = -1;
        return newIterator;

    }

    private void resetState() {
        index = -1;
    }
}