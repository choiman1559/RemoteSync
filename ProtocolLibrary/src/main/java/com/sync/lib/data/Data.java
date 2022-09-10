package com.sync.lib.data;

import java.util.Map;

public class Data {
    Map<String, String> map;
    PairDeviceInfo device;

    public Data(Map<String, String> map) {
        this.map = map;
        this.device = new PairDeviceInfo(map.get(Value.DEVICE_NAME.id()), map.get(Value.DEVICE_ID.id()));
    }

    public String get(Value type) {
        return map.get(type.id());
    }

    public String get(String type) {
        return map.get(type);
    }

    public PairDeviceInfo getDevice() {
        return device;
    }
}
