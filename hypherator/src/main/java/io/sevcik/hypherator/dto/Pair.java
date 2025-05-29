package io.sevcik.hypherator.dto;

/**
 * A generic container class that holds two related values, possibly of different types.
 *
 * @param <T1> the type of the first value
 * @param <T2> the type of the second value
 */
public class Pair<T1, T2> {
    private T1 first;
    private T2 second;

    /**
     * Constructs a pair with the given values.
     *
     * @param first  the first value
     * @param second the second value
     */
    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Constructs an empty pair. Values can be set later.
     */
    public Pair() {
    }

    /**
     * Returns the first value.
     *
     * @return the first value
     */
    public T1 getFirst() {
        return first;
    }

    /**
     * Returns the second value.
     *
     * @return the second value
     */
    public T2 getSecond() {
        return second;
    }

    /**
     * Sets the first value.
     *
     * @param first the value to set as first
     */
    public void setFirst(T1 first) {
        this.first = first;
    }

    /**
     * Sets the second value.
     *
     * @param second the value to set as second
     */
    public void setSecond(T2 second) {
        this.second = second;
    }
}