package com.sync.lib.action;

import com.sync.lib.data.Data;
import com.sync.lib.data.PairDeviceInfo;

import java.util.ArrayList;
import java.util.Map;

final public class PairListener {
    public static onDeviceFoundListener m_onDeviceFoundListener;
    public static onDevicePairResultListener m_onDevicePairResultListener;
    public static onDeviceListChangedListener m_onDeviceListChangedListener;
    public static ArrayList<onDataReceivedListener> m_onDataReceivedListener = new ArrayList<>();

    public interface onDeviceFoundListener {
        void onReceive(Data map);
    }

    public interface onDevicePairResultListener {
        void onReceive(Data map);
    }

    public interface onDataReceivedListener {
        void onReceive(Data map);
    }

    public interface onDeviceListChangedListener {
        void onReceive(ArrayList<PairDeviceInfo> list);
    }

    public static void setOnDeviceFoundListener(onDeviceFoundListener mOnDeviceFoundListener) {
        PairListener.m_onDeviceFoundListener = mOnDeviceFoundListener;
    }

    public static void setOnDevicePairResultListener(onDevicePairResultListener mOnDevicePairResultListener) {
        PairListener.m_onDevicePairResultListener = mOnDevicePairResultListener;
    }

    public static void setOnDeviceListChangedListener(onDeviceListChangedListener mOnDeviceListChangedListener) {
        PairListener.m_onDeviceListChangedListener = mOnDeviceListChangedListener;
    }

    public static void addOnDataReceivedListener(onDataReceivedListener mOnDataReceivedListener) {
        if(!m_onDataReceivedListener.contains(mOnDataReceivedListener)) m_onDataReceivedListener.add(mOnDataReceivedListener);
    }

    public static void callOnDataReceived(Data map) {
        if(m_onDataReceivedListener != null) {
            for (onDataReceivedListener listener : m_onDataReceivedListener) {
                listener.onReceive(map);
            }
        }
    }
}
