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
    public static void requestDeviceListWidely(Context context) {
        Protocol.isFindingDeviceToPair = true;
        String Topic = "/topics/" + Protocol.connectionOption.getPairingKey();
        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type","pair|request_device_list");
            notificationBody.put("device_name", Build.MANUFACTURER  + " " + Build.MODEL);
            notificationBody.put("device_id", Protocol.getConnectionOption().getIdentifierValue());
            notificationHead.put("to",Topic);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage() );
        }
        sendNotification(notificationHead, "pair.func", context);
        if(isShowDebugLog(context)) Log.d("sync sent","request list: " + notificationBody);
    }

    public static void responseDeviceInfoToFinder(Map<String, String> map, Context context) {
        String Topic = "/topics/" + Protocol.connectionOption.getPairingKey();
        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type","pair|response_device_list");
            notificationBody.put("device_name", Build.MANUFACTURER  + " " + Build.MODEL);
            notificationBody.put("device_id", Protocol.getConnectionOption().getIdentifierValue());
            notificationBody.put("send_device_name", map.get("device_name"));
            notificationBody.put("send_device_id", map.get("device_id"));
            notificationHead.put("to",Topic);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage() );
        }
        sendNotification(notificationHead, "pair.func", context);
        if(isShowDebugLog(context)) Log.d("sync sent","response list: " + notificationBody);
    }

    public static void onReceiveDeviceInfo(Map<String, String> map) {
        if(m_onDeviceFoundListener != null) m_onDeviceFoundListener.onReceive(map);
    }

    public static void requestPair(String Device_name, String Device_id, Context context) {
        String Topic = "/topics/" + Protocol.connectionOption.getPairingKey();
        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type","pair|request_pair");
            notificationBody.put("device_name", Build.MANUFACTURER  + " " + Build.MODEL);
            notificationBody.put("device_id", Protocol.getConnectionOption().getIdentifierValue());
            notificationBody.put("send_device_name", Device_name);
            notificationBody.put("send_device_id", Device_id);
            notificationHead.put("to",Topic);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage() );
        }
        sendNotification(notificationHead, "pair.func", context);
        if(isShowDebugLog(context)) Log.d("sync sent","request pair: " + notificationBody);
    }

    public static void responsePairAcceptation(PairDeviceInfo device, boolean isAccepted, Context context) {
        if(isAccepted) {
            for(PairDeviceInfo info : Protocol.pairingProcessList) {
                if(info.getDevice_name().equals(device.getDevice_name()) && info.getDevice_id().equals(device.getDevice_id())) {
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
            notificationBody.put("type","pair|accept_pair");
            notificationBody.put("device_name", Build.MANUFACTURER  + " " + Build.MODEL);
            notificationBody.put("device_id", Protocol.getConnectionOption().getIdentifierValue());
            notificationBody.put("send_device_name", device.getDevice_name());
            notificationBody.put("send_device_id", device.getDevice_id());
            notificationBody.put("pair_accept", isAccepted);
            notificationHead.put("to",Topic);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage() );
        }

        DataUtils.sendNotification(notificationHead, "pair.func", context);
    }

    public static void checkPairResultAndRegister(Map<String, String> map, PairDeviceInfo info, Context context) {
        if(isShowDebugLog(context)) Log.i("pair result", "device name: " + map.get("device_name") + " /device id: " + map.get("device_id") + " /result: " + map.get("pair_accept"));
        if(m_onDevicePairResultListener != null) m_onDevicePairResultListener.onReceive(map);
        if("true".equals(map.get("pair_accept"))) {
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

    public static boolean isShowDebugLog(Context context) {
        return context.getSharedPreferences("com.sync.protocol_preferences", MODE_PRIVATE).getBoolean("printDebugLog", false);
    }
}
