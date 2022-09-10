package com.sync.lib.data;

import androidx.annotation.NonNull;

import java.util.Objects;

public class PairDeviceInfo {
    /**
     * device name information
     */
    private final String Device_name;

    /**
     * device unique id information
     */
    private final String Device_id;

    /**
     * device connection status information
     */
    @PairDeviceStatus.Status
    private int Device_status;

    public PairDeviceInfo(String Device_name, String Device_id, @PairDeviceStatus.Status int Device_status) {
        this.Device_id = Device_id;
        this.Device_name = Device_name;
        this.Device_status = Device_status;
    }

    public PairDeviceInfo(String Device_name, String Device_id) {
        this.Device_id = Device_id;
        this.Device_name = Device_name;
        this.Device_status = PairDeviceStatus.Device_Already_Paired;
    }

    public String getDevice_id() {
        return Device_id;
    }

    public String getDevice_name() {
        return Device_name;
    }

    public int getDevice_status() {
        return Device_status;
    }

    public PairDeviceInfo setDevice_status(@PairDeviceStatus.Status int Device_status) {
        this.Device_status = Device_status;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return Device_name + "|" + Device_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PairDeviceInfo that = (PairDeviceInfo) o;
        return Objects.equals(Device_name, that.Device_name) && Objects.equals(Device_id, that.Device_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Device_name, Device_id);
    }
}
