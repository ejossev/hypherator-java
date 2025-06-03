package io.sevcik.hypherator;

import io.sevcik.hypherator.dto.Pair;
import io.sevcik.hypherator.dto.PotentialBreak;

import java.util.List;

/**
 * Interface for direct interaction with hyphenation logic.<br><br>
 * 
 * <b>Note:</b> In most cases, it is recommended to use the provided {@link HyphenationIterator} rather than this interface directly.
 * The iterator offers a more user-friendly and idiomatic way to traverse and apply hyphenation points, abstracting away implementation details
 * and improving future compatibility.
 * <br><br>
 * This interface is primarily intended for advanced or internal use.
 */
public interface Hyphenate {

    /**
     * Returns a list of hyphenation opportunities ({@link PotentialBreak}) for the given text using the specified dictionary.<br>
     * <b>Prefer using {@link HyphenationIterator} to process hyphenation points.</b>
     *
     * @param dict the hyphenation dictionary
     * @param text the input word or text
     * @return a list of hyphenation breaks (opaque handles)
     */
    List<PotentialBreak> hyphenate(HyphenDict dict, String text);

    /**
     * Applies a given {@link PotentialBreak} to the input text, returning the result as a pair (before and after the hyphenation point).<br>
     * <b>Prefer using {@link HyphenationIterator} to process and apply hyphenation points.</b>
     *
     * @param text the original text
     * @param breakRule the potential break to be applied (opaque object)
     * @return a pair containing the text before and after the break
     */
    Pair<String, String> applyBreak(String text, PotentialBreak breakRule);

    /**
     * Returns a list of hyphenation opportunities ({@link PotentialBreak}) for the right part of the text already hyphenated.
     * <b>Prefer using {@link io.sevcik.hypherator.HyphenationIterator} to process and apply hyphenation points.</b>
     *
     * @param dict the hyphenation dictionary
     * @param currentBreaks the breaks identified in previous iteration
     * @param breakPosition the position of the break point
     */
    List<PotentialBreak> getFurtherHyphenations(HyphenDict dict, List<PotentialBreak> currentBreaks, PotentialBreak breakPosition, String rightPart);
}