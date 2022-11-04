package com.sync.lib.util;

import com.sync.lib.Protocol;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class DataReadWriter {
    public static final String DEFAULT_DATASET_KEY = "paired_list";

    public Set<String> readData(String key) {
        return readData(key, new HashSet<>());
    }

    public Set<String> readData(String key, Collection<String> defaultValue) {
        return Protocol.getInstance().pairPrefs.getStringSet(key, (Set<String>) defaultValue);
    }

    public void writeData(String key, Collection<String> value) {
        if(value instanceof Set) Protocol.getInstance().pairPrefs.edit().putStringSet(key, (Set<String>) value).apply();
        else throw new IllegalArgumentException("value is null or cannot be instance of Set<String>!");
    }
}
