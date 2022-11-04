package com.sync.protocol.service;

import com.sync.lib.util.DataReadWriter;

import java.util.Collection;
import java.util.Set;

public class DataReadWriteImpl extends DataReadWriter {
    @Override
    public Set<String> readData(String key) {
        return super.readData(key);
    }

    @Override
    public Set<String> readData(String key, Collection<String> defaultValue) {
        return super.readData(key, defaultValue);
    }

    @Override
    public void writeData(String key, Collection<String> value) {
        super.writeData(key, value);
    }
}
