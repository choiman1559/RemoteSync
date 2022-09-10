package com.sync.lib.process;

import static android.content.Context.MODE_PRIVATE;
import static com.sync.lib.action.PairListener.m_onDeviceFoundListener;
import static com.sync.lib.action.PairListener.m_onDevicePairResultListener;
import static com.sync.lib.util.DataUtils.sendNotification;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.sync.lib.Protocol;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.util.DataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Process {
    /**
     * Initiation of pairing: request device information from all devices that can receive data
     *
     * @param context android context instance
     */
    public static void requestDeviceListWidely(Context context) {
        Protocol.isFindingDeviceToPair = true;
        String Topic = "/topics/" + Protocol.connectionOption.getPairingKey();
        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "pair|request_device_list");
            notificationBody.put("device_name", Build.MANUFACTURER + " " + Build.MODEL);
            notificationBody.put("device_id", Protocol.getConnectionOption().getIdentifierValue());
            notificationHead.put("to", Topic);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        sendNotification(notificationHead, "pair.func", context, true);
        if (isShowDebugLog()) Log.d("sync sent", "request list: " + notificationBody);
    }

    /**
     * When a device send request is received, this device's information is sent to the device that sent the request.
     *
     * @param map Raw data from FCM
     * @param context android context instance
     */
    public static void responseDeviceInfoToFinder(Map<String, String> map, Context context) {
        String Topic = "/topics/" + Protocol.connectionOption.getPairingKey();
        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "pair|response_device_list");
            notificationBody.put("device_name", Build.MANUFACTURER + " " + Build.MODEL);
            notificationBody.put("device_id", Protocol.getConnectionOption().getIdentifierValue());
            notificationBody.put("send_device_name", map.get("device_name"));
            notificationBody.put("send_device_id", map.get("device_id"));
            notificationHead.put("to", Topic);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        sendNotification(notificationHead, "pair.func", context);
        if (isShowDebugLog()) Log.d("sync sent", "response list: " + notificationBody);
    }

    /**
     * When the requested device information is received, the listener is called.
     *
     * @param map Raw data from FCM
     */
    public static void onReceiveDeviceInfo(Map<String, String> map) {
        if (m_onDeviceFoundListener != null) m_onDeviceFoundListener.onReceive(map);
    }

    /**
     * When a device send request is received, this device's information is sent to the device that sent the request.
     *
     * @param device target device to request pair
     * @param context android context instance
     */
    public static void requestPair(PairDeviceInfo device, Context context) {
        String Topic = "/topics/" + Protocol.connectionOption.getPairingKey();
        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "pair|request_pair");
            notificationBody.put("device_name", Build.MANUFACTURER + " " + Build.MODEL);
            notificationBody.put("device_id", Protocol.getConnectionOption().getIdentifierValue());
            notificationBody.put("send_device_name", device.getDevice_name());
            notificationBody.put("send_device_id", device.getDevice_id());
            notificationHead.put("to", Topic);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        sendNotification(notificationHead, "pair.func", context);
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

        String Topic = "/topics/" + Protocol.connectionOption.getPairingKey();
        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "pair|accept_pair");
            notificationBody.put("device_name", Build.MANUFACTURER + " " + Build.MODEL);
            notificationBody.put("device_id", Protocol.getConnectionOption().getIdentifierValue());
            notificationBody.put("send_device_name", device.getDevice_name());
            notificationBody.put("send_device_id", device.getDevice_id());
            notificationBody.put("pair_accept", isAccepted ? "true" : "false");
            notificationHead.put("to", Topic);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }

        DataUtils.sendNotification(notificationHead, "pair.func", context);
    }

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
     * @param context android context instance
     */
    public static void checkPairResultAndRegister(Map<String, String> map, PairDeviceInfo info, Context context) {
        if (m_onDevicePairResultListener != null) m_onDevicePairResultListener.onReceive(map);
        if ("true".equals(map.get("pair_accept"))) {
            SharedPreferences prefs = context.getSharedPreferences("com.sync.protocol_pair", MODE_PRIVATE);
            boolean isNotRegistered = true;
            String dataToSave = map.get("device_name") + "|" + map.get("device_id");

            Set<String> list = new HashSet<>(prefs.getStringSet("paired_list", new HashSet<>()));
            for(String str : list) {
                if(str.equals(dataToSave)) {
                    isNotRegistered = false;
                    break;
                }
            }

            if(isNotRegistered) {
                list.add(dataToSave);
                prefs.edit().putStringSet("paired_list", list).apply();
            }

            Protocol.isFindingDeviceToPair = false;
            Protocol.pairingProcessList.remove(info);
        }
    }

    /**
     * Device information will be deleted from this device when requested to unpair.
     *
     * @param map Raw data from FCM
     */
    public static void removePairedDevice(Map<String, String> map) {
        Set<String> list = new HashSet<>(Protocol.pairPrefs.getStringSet("paired_list", new HashSet<>()));
        list.remove(map.get("device_name") + "|" + map.get("device_id"));
        Protocol.pairPrefs.edit().putStringSet("paired_list", list).apply();
        Protocol.action.onPairRemoved(map);
    }

    /**
     * Send an unpairing request to another device.
     *
     * @param device target device to disconnect pair
     * @param context android context instance
     */
    public static void requestRemovePair(Context context, PairDeviceInfo device) {
        Set<String> list = new HashSet<>(Protocol.pairPrefs.getStringSet("paired_list", new HashSet<>()));
        list.remove(device.getDevice_name() + "|" + device.getDevice_id());
        Protocol.pairPrefs.edit().putStringSet("paired_list", list).apply();

        String Topic = "/topics/" + Protocol.connectionOption.getPairingKey();
        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "pair|request_remove");
            notificationBody.put("device_name", Build.MANUFACTURER + " " + Build.MODEL);
            notificationBody.put("device_id", Protocol.getConnectionOption().getIdentifierValue());
            notificationBody.put("send_device_name", device.getDevice_name());
            notificationBody.put("send_device_id", device.getDevice_id());
            notificationHead.put("to", Topic);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        sendNotification(notificationHead, "pair.func", context);
        if (isShowDebugLog()) Log.d("sync sent", "request remove: " + notificationBody);
    }

    private static boolean isShowDebugLog() {
        return Protocol.connectionOption.isPrintDebugLog();
    }
}
