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
    /**
     * Send json data to push server
     *
     * @param notification Json data to send push server
     * @param PackageName Current working app's package name
     * @param context current Android context instance
     */
    public static void sendNotification(JSONObject notification, String PackageName, Context context) {
        sendNotification(notification, PackageName, context, false);
    }

    /**
     * Send json data to push server
     *
     * @param notification Json data to send push server
     * @param PackageName Current working app's package name
     * @param context current Android context instance
     * @param isFirstFetch Whether or not you are pitching with the target device for the first time
     */
    public static void sendNotification(JSONObject notification, String PackageName, Context context, boolean isFirstFetch) {
        final String FCM_API = "https://fcm.googleapis.com/fcm/send";
        final String serverKey = Protocol.getConnectionOption().getServerKey();
        final String contentType = "application/json";
        final String TAG = "NOTIFICATION TAG";

        try {
            String rawPassword = Protocol.getConnectionOption().getEncryptionPassword();
            JSONObject data = notification.getJSONObject("data");
            if (Protocol.connectionOption.isEncryptionEnabled() && !rawPassword.equals("")) {
                String encryptedData;
                if(Protocol.connectionOption.isAuthWithHMac()) {
                    Log.d("ddd", data.getString("send_device_id"));
                    encryptedData = AESCrypto.encrypt(notification.getJSONObject("data").toString(), rawPassword, isFirstFetch ? Protocol.connectionOption.getPairingKey() : data.getString("send_device_id"));
                } else {
                    encryptedData = AESCrypto.encrypt(notification.getJSONObject("data").toString(), rawPassword);
                }

                JSONObject newData = new JSONObject();
                newData.put("encrypted", "true");
                newData.put("isFirstFetch", isFirstFetch);
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

    /**
     * request find task to target device
     *
     * @param device target device to send
     * @param context current Android context instance
     */
    public static void sendFindTaskNotification(Context context, PairDeviceInfo device) {
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

        DataUtils.sendNotification(notificationHead, context.getPackageName(), context);
    }

    /**
     * request some data to target device
     *
     * @param device target device to send
     * @param context current Android context instance
     * @param dataType type of data to request
     */
    public static void requestData(Context context, PairDeviceInfo device, String dataType) {
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
            notificationBody.put("send_device_name", device.getDevice_name());
            notificationBody.put("send_device_id", device.getDevice_id());
            notificationBody.put("request_data", dataType);
            notificationBody.put("date", date);

            notificationHead.put("to", TOPIC);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        DataUtils.sendNotification(notificationHead, context.getPackageName(), context);
    }

    /**
     * response to the device requesting the data
     *
     * @param device target device to send
     * @param context current Android context instance
     * @param dataType type of data requested
     * @param dataContent the value of the requested data
     */
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
        DataUtils.sendNotification(notificationHead, context.getPackageName(), context);
    }

    /**
     * request some action to target device
     *
     * @param device target device to send
     * @param context current Android context instance
     * @param dataType type of data requested
     * @param args Argument data required to execute the action
     */
    public static void requestAction(Context context, PairDeviceInfo device, String dataType, String... args) {
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
            notificationBody.put("send_device_name", device.getDevice_name());
            notificationBody.put("send_device_id", device.getDevice_id());
            notificationBody.put("request_action", dataType);
            notificationBody.put("date", date);
            if (args.length > 0) notificationBody.put("action_args", dataToSend.toString());

            notificationHead.put("to", TOPIC);
            notificationHead.put("priority", "high");
            notificationHead.put("data", notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        DataUtils.sendNotification(notificationHead, context.getPackageName(), context);
    }
}
