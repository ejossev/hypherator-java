package io.sevcik.hypherator.dto;

import java.util.List;

/**
 * Dictionary entry class for JSON deserialization.
 */
public class DictionaryEntry {
    private List<String> locations;
    private List<String> locales;
    private String hyphen;

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getLocales() {
        return locales;
    }

    public void setLocales(List<String> locales) {
        this.locales = locales;
    }

    public String getHyphen() {
        return hyphen;
    }

    public DictionaryEntry setHyphen(String hyphen) {
        this.hyphen = hyphen;
        return this;
    }
}
