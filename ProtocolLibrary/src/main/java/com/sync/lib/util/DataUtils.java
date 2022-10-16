package com.sync.lib.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.sync.lib.Protocol;
import com.sync.lib.data.KeySpec;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.data.Value;

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
     * @param PackageName  Current working app's package name
     * @param context      current Android context instance
     */
    public static void sendNotification(JSONObject notification, String PackageName, Context context) {
        sendNotification(notification, PackageName, context, false);
    }

    /**
     * Send json data to push server
     *
     * @param notificationBody Json data to send push server
     * @param PackageName  Current working app's package name
     * @param context      current Android context instance
     * @param isFirstFetch Whether or not you are pitching with the target device for the first time
     */
    public static void sendNotification(JSONObject notificationBody, String PackageName, Context context, boolean isFirstFetch) {
        Protocol instance = Protocol.getInstance();

        final String FCM_API = "https://fcm.googleapis.com/fcm/send";
        final String serverKey = instance.connectionOption.getServerKey();
        final String contentType = "application/json";
        final String TAG = "NOTIFICATION TAG";

        try {
            KeySpec keySpec = instance.connectionOption.getKeySpec();
            if (instance.connectionOption.isEncryptionEnabled() && keySpec.isValidKey()) {
                if(keySpec.isAuthWithHMac()) keySpec.setSecondaryPassword(isFirstFetch ? instance.connectionOption.getPairingKey() : notificationBody.getString(Value.SEND_DEVICE_ID.id()));
                String encryptedData = Crypto.encrypt(notificationBody.toString(), keySpec);

                JSONObject newData = new JSONObject();
                newData.put(Value.ENCRYPTED.id(), "true");
                newData.put(Value.IS_FIRST_FETCHED.id(), String.valueOf(isFirstFetch));
                if(!isFirstFetch) newData.put(Value.SEND_DEVICE_NAME.id(), notificationBody.get(Value.SEND_DEVICE_NAME.id()));
                notificationBody = newData.put(Value.ENCRYPTED_DATA.id(), CompressStringUtil.compressString(encryptedData));
            } else {
                notificationBody = notificationBody.put(Value.ENCRYPTED.id(), "false");
            }
        } catch (Exception e) {
            if (instance.connectionOption.isPrintDebugLog()) e.printStackTrace();
        }

        JSONObject notification = new JSONObject();
        try {
            String Topic = "/topics/" + instance.connectionOption.getPairingKey();
            notification.put(Value.TOPIC.id(), Topic);
            notification.put(Value.PRIORITY.id(), "high");
            notification.put(Value.DATA.id(), notificationBody);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
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
     * @param device  target device to send
     * @param context current Android context instance
     */
    public static void sendFindTaskNotification(Context context, PairDeviceInfo device) {
        Protocol instance = Protocol.getInstance();
        Date date = Calendar.getInstance().getTime();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|find");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.SENT_DATE.id(), date);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }

        DataUtils.sendNotification(notificationBody, context.getPackageName(), context);
    }

    /**
     * request some data to target device
     *
     * @param device   target device to send
     * @param context  current Android context instance
     * @param dataType type of data to request
     */
    public static void requestData(Context context, PairDeviceInfo device, String dataType) {
        Protocol instance = Protocol.getInstance();
        Date date = Calendar.getInstance().getTime();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_data");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.REQUEST_DATA.id(), dataType);
            notificationBody.put(Value.SENT_DATE.id(), date);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        DataUtils.sendNotification(notificationBody, context.getPackageName(), context);
    }

    /**
     * response to the device requesting the data
     *
     * @param device      target device to send
     * @param context     current Android context instance
     * @param dataType    type of data requested
     * @param dataContent the value of the requested data
     */
    public static void responseDataRequest(PairDeviceInfo device, String dataType, String dataContent, Context context) {
        Protocol instance = Protocol.getInstance();
        Date date = Calendar.getInstance().getTime();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|receive_data");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.RECEIVE_DATA.id(), dataContent);
            notificationBody.put(Value.REQUEST_DATA.id(), dataType);
            notificationBody.put(Value.SENT_DATE.id(), date);
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        DataUtils.sendNotification(notificationBody, context.getPackageName(), context);
    }

    /**
     * request some action to target device
     *
     * @param device   target device to send
     * @param context  current Android context instance
     * @param dataType type of data requested
     * @param args     Argument data required to execute the action
     */
    public static void requestAction(Context context, PairDeviceInfo device, String dataType, String... args) {
        Protocol instance = Protocol.getInstance();
        StringBuilder dataToSend = new StringBuilder();
        if (args.length > 1) {
            for (String str : args) {
                dataToSend.append(str).append("|");
            }
            dataToSend.setCharAt(dataToSend.length() - 1, '\0');
        } else if (args.length == 1) dataToSend.append(args[0]);

        Date date = Calendar.getInstance().getTime();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_action");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.REQUEST_ACTION.id(), dataType);
            notificationBody.put(Value.SENT_DATE.id(), date);
            if (args.length > 0) notificationBody.put(Value.ACTION_ARGS.id(), dataToSend.toString());
        } catch (JSONException e) {
            Log.e("Noti", "onCreate: " + e.getMessage());
        }
        DataUtils.sendNotification(notificationBody, context.getPackageName(), context);
    }
}
