package com.sync.lib.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.sync.lib.Protocol;
import com.sync.lib.data.PairDeviceInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DataUtils {
    public static void sendNotification(JSONObject notification, String PackageName, Context context) {
        final String FCM_API = "https://fcm.googleapis.com/fcm/send";
        final String serverKey = Protocol.getConnectionOption().getServerKey();
        final String contentType = "application/json";
        final String TAG = "NOTIFICATION TAG";

        try {
            String rawPassword = Protocol.getConnectionOption().getEncryptionPassword();
            JSONObject data = notification.getJSONObject("data");
            if (Protocol.connectionOption.isEncryptionEnabled() && !rawPassword.equals("")) {
                String encryptedData = AESCrypto.encrypt(notification.getJSONObject("data").toString(), rawPassword);

                JSONObject newData = new JSONObject();
                newData.put("encrypted", "true");
                newData.put("encryptedData", CompressStringUtil.compressString(encryptedData));
                notification.put("data", newData);
            } else {
                data.put("encrypted", "false");
                notification.put("data", data);
            }
        } catch (Exception e) {
            if (Protocol.connectionOption.isPrintDebugLog()) e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, FCM_API, notification,
                response -> Log.i(TAG, "onResponse: " + response.toString() + " ,package: " + PackageName),
                error -> {
                    Toast.makeText(context, "Failed to send Notification! Please check internet and try again!", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onErrorResponse: Didn't work" + " ,package: " + PackageName);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", serverKey);
                params.put("Content-Type", contentType);
                return params;
            }
        };
        JsonRequest.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

    public static void sendFindTaskNotification(PairDeviceInfo device, Context context) {
        Date date = Calendar.getInstance().getTime();
        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = Protocol.connectionOption.getIdentifierValue();
        String TOPIC = "/topics/" + Protocol.connectionOption.getPairingKey();

        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "pair|find");
            notificationBody.put("device_name", DEVICE_NAME);
            notificationBody.put("device_id", DEVICE_ID);
            notificationBody.put("send_device_name", device.getDevice_name());
            notificationBody.put("send_device_id", device.getDevice_id());
            notificationBody.put("date", date);

            notificationHead.put("to", TOPIC);
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }

        com.sync.lib.util.DataUtils.sendNotification(notificationHead, context.getPackageName(), context);
    }

    public static void sendFindTaskNotification(Context context) {
        boolean isLogging = Protocol.connectionOption.isPrintDebugLog();
        Date date = Calendar.getInstance().getTime();

        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = Protocol.connectionOption.getIdentifierValue();
        String TOPIC = "/topics/" + Protocol.connectionOption.getPairingKey();

        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "send|find");
            notificationBody.put("device_name", DEVICE_NAME);
            notificationBody.put("device_id", DEVICE_ID);
            notificationBody.put("date", date);

            notificationHead.put("to", TOPIC);
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            if (isLogging) Log.e("Noti", "onCreate: " + e.getMessage());
        }
        if (isLogging) Log.d("data", notificationHead.toString());
        sendNotification(notificationHead, context.getPackageName(), context);
    }

    public static void requestData(Context context, String Device_name, String Device_id, String dataType) {
        Date date = Calendar.getInstance().getTime();
        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = Protocol.connectionOption.getIdentifierValue();
        String TOPIC = "/topics/" + Protocol.connectionOption.getPairingKey();

        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "pair|request_data");
            notificationBody.put("device_name", DEVICE_NAME);
            notificationBody.put("device_id", DEVICE_ID);
            notificationBody.put("send_device_name", Device_name);
            notificationBody.put("send_device_id", Device_id);
            notificationBody.put("request_data", dataType);
            notificationBody.put("date", date);

            notificationHead.put("to", TOPIC);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        com.sync.lib.util.DataUtils.sendNotification(notificationHead, context.getPackageName(), context);
    }

    public static void responseDataRequest(PairDeviceInfo device, String dataType, String dataContent, Context context) {
        Date date = Calendar.getInstance().getTime();
        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = Protocol.connectionOption.getIdentifierValue();
        String TOPIC = "/topics/" + Protocol.connectionOption.getPairingKey();

        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "pair|receive_data");
            notificationBody.put("device_name", DEVICE_NAME);
            notificationBody.put("device_id", DEVICE_ID);
            notificationBody.put("send_device_name", device.getDevice_name());
            notificationBody.put("send_device_id", device.getDevice_id());
            notificationBody.put("receive_data", dataContent);
            notificationBody.put("request_data", dataType);
            notificationBody.put("date", date);

            notificationHead.put("to", TOPIC);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        com.sync.lib.util.DataUtils.sendNotification(notificationHead, context.getPackageName(), context);
    }

    public static void requestAction(Context context, String Device_name, String Device_id, String dataType, String... args) {
        StringBuilder dataToSend = new StringBuilder();
        if (args.length > 1) {
            for (String str : args) {
                dataToSend.append(str).append("|");
            }
            dataToSend.setCharAt(dataToSend.length() - 1, '\0');
        } else if (args.length == 1) dataToSend.append(args[0]);

        Date date = Calendar.getInstance().getTime();
        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = Protocol.connectionOption.getIdentifierValue();
        String TOPIC = "/topics/" + Protocol.connectionOption.getPairingKey();

        JSONObject notificationHead = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        try {
            notificationBody.put("type", "pair|request_action");
            notificationBody.put("device_name", DEVICE_NAME);
            notificationBody.put("device_id", DEVICE_ID);
            notificationBody.put("send_device_name", Device_name);
            notificationBody.put("send_device_id", Device_id);
            notificationBody.put("request_action", dataType);
            notificationBody.put("date", date);
            if (args.length > 0) notificationBody.put("action_args", dataToSend.toString());

            notificationHead.put("to", TOPIC);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        com.sync.lib.util.DataUtils.sendNotification(notificationHead, context.getPackageName(), context);
    }
}
