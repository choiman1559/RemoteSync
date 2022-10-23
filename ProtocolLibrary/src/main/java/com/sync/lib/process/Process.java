package com.sync.lib.process;

import static com.sync.lib.action.PairListener.m_onDeviceFoundListener;
import static com.sync.lib.action.PairListener.m_onDevicePairResultListener;
import static com.sync.lib.util.DataUtils.pushErrorResultToListener;
import static com.sync.lib.util.DataUtils.sendNotification;

import android.util.Log;

import com.sync.lib.Protocol;
import com.sync.lib.data.Data;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.data.Value;
import com.sync.lib.task.RequestTask;
import com.sync.lib.util.DataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class Process {
    /**
     * Initiation of pairing: request device information from all devices that can receive data
     *
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask requestDeviceListWidely() {
        Protocol instance = Protocol.getInstance();
        instance.isFindingDeviceToPair = true;
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_device_list");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
        } catch (JSONException e) {
            return pushErrorResultToListener(e);
        }

        if (isShowDebugLog()) Log.d("sync sent", "request list: " + notificationBody);
        return sendNotification(notificationBody, true);
    }

    /**
     * When a device send request is received, this device's information is sent to the device that sent the request.
     *
     * @param map Raw data from FCM
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask responseDeviceInfoToFinder(Data map) {
        Protocol instance = Protocol.getInstance();
        JSONObject notificationBody = new JSONObject();
        PairDeviceInfo device = map.getDevice();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|response_device_list");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
        } catch (JSONException e) {
            return pushErrorResultToListener(e);
        }

        if (isShowDebugLog()) Log.d("sync sent", "response list: " + notificationBody);
        return sendNotification(notificationBody);
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
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask requestPair(PairDeviceInfo device) {
        Protocol instance = Protocol.getInstance();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_pair");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
        } catch (JSONException e) {
            return pushErrorResultToListener(e);
        }

        if (isShowDebugLog()) Log.d("sync sent", "request pair: " + notificationBody);
        return sendNotification(notificationBody);
    }

    /**
     * Sends the result of the user deciding whether to pair or not to the device that requested pairing.
     *
     * @param device target device to response pair
     * @param isAccepted Whether the user accepts the pairing
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask responsePairAcceptation(PairDeviceInfo device, boolean isAccepted) {
        Protocol instance = Protocol.getInstance();
        if (isAccepted) {
            registerDevice(device);
            for (PairDeviceInfo info : instance.pairingProcessList) {
                if (info.getDevice_name().equals(device.getDevice_name()) && info.getDevice_id().equals(device.getDevice_id())) {
                    instance.isListeningToPair = false;
                    instance.pairingProcessList.remove(info);
                    break;
                }
            }
        }

        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put(Value.TYPE.id(), "pair|accept_pair");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.PAIR_ACCEPT.id(), isAccepted ? "true" : "false");
        } catch (JSONException e) {
            return pushErrorResultToListener(e);
        }

        return DataUtils.sendNotification(notificationBody);
    }

    /**
     * Save device name and id value to preferences
     *
     * @param device target device to save in preferences
     */
    protected static void registerDevice(PairDeviceInfo device) {
        Protocol instance = Protocol.getInstance();
        boolean isNotRegistered = true;
        String dataToSave = device.toString();

        Set<String> list = new HashSet<>(instance.pairPrefs.getStringSet("paired_list", new HashSet<>()));
        for(String str : list) {
            if(str.equals(dataToSave)) {
                isNotRegistered = false;
                break;
            }
        }

        if(isNotRegistered) {
            list.add(dataToSave);
            instance.pairPrefs.edit().putStringSet("paired_list", list).apply();
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
            Protocol instance = Protocol.getInstance();
            instance.isFindingDeviceToPair = false;
            instance.pairingProcessList.remove(info);
        }
    }

    /**
     * Device information will be deleted from this device when requested to unpair.
     *
     * @param device Device to delete
     * @param haveToAnnounce Set whether call "onPairRemoved" listener after delete device
     */
    public static void removePairedDevice(PairDeviceInfo device, boolean haveToAnnounce) {
        Protocol instance = Protocol.getInstance();
        Set<String> list = new HashSet<>(Protocol.getInstance().pairPrefs.getStringSet("paired_list", new HashSet<>()));
        list.remove(device.toString());
        instance.pairPrefs.edit().putStringSet("paired_list", list).apply();
        if(haveToAnnounce) instance.action.onPairRemoved(device);
    }

    /**
     * Send an unpairing request to another device.
     *
     * @param device target device to disconnect pair
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask requestRemovePair(PairDeviceInfo device) {
        Protocol instance = Protocol.getInstance();
        removePairedDevice(device, false);
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_remove");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
        } catch (JSONException e) {
            return pushErrorResultToListener(e);
        }
        if (isShowDebugLog()) Log.d("sync sent", "request remove: " + notificationBody);
        return sendNotification(notificationBody);
    }

    private static boolean isShowDebugLog() {
        return Protocol.getInstance().connectionOption.isPrintDebugLog();
    }
}
