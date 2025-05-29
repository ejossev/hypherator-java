package io.sevcik.hypherator;

import io.sevcik.hypherator.dto.Pair;
import io.sevcik.hypherator.dto.PotentialBreak;

/**
 * An iterator interface for traversing possible hyphenation points in a word.
 */
public interface HyphenationIterator {
    /**
     * Sentinel value indicating no further hyphenation points are available.
     */
    PotentialBreak DONE = null;

    /**
     * Returns the first valid hyphenation point, or {@code DONE} if none. Resets
     *
     * @return the first available {@link PotentialBreak}, or {@code DONE}
     */
    PotentialBreak first();

    /**
     * Returns the next valid hyphenation point, or {@code DONE} if at the end.
     *
     * @return the next available {@link PotentialBreak}, or {@code DONE}
     */
    PotentialBreak next();

    /**
     * Sets the urgency level, which may affect which hyphenation points are considered.
     *
     * @param urgency the urgency level
     */
    void setUrgency(int urgency);

    /**
     * Sets the word to be iterated for hyphenation.
     *
     * @param word the word to hyphenate
     */
    void setWord(String word);

    /**
     * Applies the given hyphenation break to the current word.
     *
     * @param breakRule the hyphenation point to apply
     * @return a pair containing the two parts of the split word
     */
    Pair<String, String> applyBreak(PotentialBreak breakRule);
}