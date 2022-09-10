package com.sync.lib.process;

import static com.sync.lib.action.PairListener.m_onDeviceFoundListener;
import static com.sync.lib.action.PairListener.m_onDevicePairResultListener;
import static com.sync.lib.util.DataUtils.sendNotification;

import android.content.Context;
import android.util.Log;

import com.sync.lib.Protocol;
import com.sync.lib.data.Data;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.data.Value;
import com.sync.lib.util.DataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class Process {
    /**
     * Initiation of pairing: request device information from all devices that can receive data
     *
     * @param context android context instance
     */
    public static void requestDeviceListWidely(Context context) {
        Protocol.isFindingDeviceToPair = true;
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_device_list");
            notificationBody.put(Value.DEVICE_NAME.id(), Protocol.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), Protocol.thisDevice.getDevice_id());
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        sendNotification(notificationBody, "pair.func", context, true);
        if (isShowDebugLog()) Log.d("sync sent", "request list: " + notificationBody);
    }

    /**
     * When a device send request is received, this device's information is sent to the device that sent the request.
     *
     * @param map Raw data from FCM
     * @param context android context instance
     */
    public static void responseDeviceInfoToFinder(Data map, Context context) {
        JSONObject notificationBody = new JSONObject();
        PairDeviceInfo device = map.getDevice();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|response_device_list");
            notificationBody.put(Value.DEVICE_NAME.id(), Protocol.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), Protocol.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        sendNotification(notificationBody, "pair.func", context);
        if (isShowDebugLog()) Log.d("sync sent", "response list: " + notificationBody);
    }

    /**
     * When the requested device information is received, the listener is called.
     *
     * @param map Raw data from FCM
     */
    public static void onReceiveDeviceInfo(Data map) {
        if (m_onDeviceFoundListener != null) m_onDeviceFoundListener.onReceive(map);
    }

    /**
     * When a device send request is received, this device's information is sent to the device that sent the request.
     *
     * @param device target device to request pair
     * @param context android context instance
     */
    public static void requestPair(PairDeviceInfo device, Context context) {
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_pair");
            notificationBody.put(Value.DEVICE_NAME.id(), Protocol.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), Protocol.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        sendNotification(notificationBody, "pair.func", context);
        if (isShowDebugLog()) Log.d("sync sent", "request pair: " + notificationBody);
    }

    /**
     * Sends the result of the user deciding whether to pair or not to the device that requested pairing.
     *
     * @param device target device to response pair
     * @param isAccepted Whether the user accepts the pairing
     * @param context android context instance
     */
    public static void responsePairAcceptation(PairDeviceInfo device, boolean isAccepted, Context context) {
        if (isAccepted) {
            registerDevice(device);
            for (PairDeviceInfo info : Protocol.pairingProcessList) {
                if (info.getDevice_name().equals(device.getDevice_name()) && info.getDevice_id().equals(device.getDevice_id())) {
                    Protocol.isListeningToPair = false;
                    Protocol.pairingProcessList.remove(info);
                    break;
                }
            }
        }

        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put(Value.TYPE.id(), "pair|accept_pair");
            notificationBody.put(Value.DEVICE_NAME.id(), Protocol.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), Protocol.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.PAIR_ACCEPT.id(), isAccepted ? "true" : "false");
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }

        DataUtils.sendNotification(notificationBody, "pair.func", context);
    }

    /**
     * Save device name and id value to preferences
     *
     * @param device target device to save in preferences
     */
    protected static void registerDevice(PairDeviceInfo device) {
        boolean isNotRegistered = true;
        String dataToSave = device.toString();

        Set<String> list = new HashSet<>(Protocol.pairPrefs.getStringSet("paired_list", new HashSet<>()));
        for(String str : list) {
            if(str.equals(dataToSave)) {
                isNotRegistered = false;
                break;
            }
        }

        if(isNotRegistered) {
            list.add(dataToSave);
            Protocol.pairPrefs.edit().putStringSet("paired_list", list).apply();
        }
    }

    /**
     * The pairing is processed after receiving the pairing acceptance from the requested device.
     *
     * @param info target device to response pair
     * @param map Raw data from FCM
     */
    public static void checkPairResultAndRegister(Data map, PairDeviceInfo info) {
        if (m_onDevicePairResultListener != null) m_onDevicePairResultListener.onReceive(map);
        if ("true".equals(map.get(Value.PAIR_ACCEPT))) {
            registerDevice(info);
            Protocol.isFindingDeviceToPair = false;
            Protocol.pairingProcessList.remove(info);
        }
    }

    /**
     * Device information will be deleted from this device when requested to unpair.
     *
     * @param device Device to delete
     * @param haveToAnnounce Set whether call "onPairRemoved" listener after delete device
     */
    public static void removePairedDevice(PairDeviceInfo device, boolean haveToAnnounce) {
        Set<String> list = new HashSet<>(Protocol.pairPrefs.getStringSet("paired_list", new HashSet<>()));
        list.remove(device.toString());
        Protocol.pairPrefs.edit().putStringSet("paired_list", list).apply();
        if(haveToAnnounce) Protocol.action.onPairRemoved(device);
    }

    /**
     * Send an unpairing request to another device.
     *
     * @param device target device to disconnect pair
     * @param context android context instance
     */
    public static void requestRemovePair(Context context, PairDeviceInfo device) {
        removePairedDevice(device, false);
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_remove");
            notificationBody.put(Value.DEVICE_NAME.id(), Protocol.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), Protocol.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        sendNotification(notificationBody, "pair.func", context);
        if (isShowDebugLog()) Log.d("sync sent", "request remove: " + notificationBody);
    }

    private static boolean isShowDebugLog() {
        return Protocol.connectionOption.isPrintDebugLog();
    }
}
