package com.sync.protocol.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.sync.protocol.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DataUtils {
    @SuppressLint("HardwareIds")
    public static String getUniqueID(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String str = "";
        if (prefs != null) {
            switch (prefs.getString("uniqueIdMethod", "Globally-Unique ID")) {
                case "Globally-Unique ID":
                    str = prefs.getString("GUIDPrefix", "");
                    break;

                case "Android ID":
                    str = prefs.getString("AndroidIDPrefix", "");
                    break;

                case "Firebase IID":
                    str = prefs.getString("FirebaseIIDPrefix", "");
                    break;

                case "Device MAC ID":
                    str = prefs.getString("MacIDPrefix", "");
                    break;
            }
            return str;
        }
        return "";
    }

    public static void sendFindTaskNotification(Context context) {
        boolean isLogging = BuildConfig.DEBUG;
        Date date = Calendar.getInstance().getTime();
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        if (prefs == null) prefs = context.getSharedPreferences("com.sync.protocol_preferences", Context.MODE_PRIVATE);

        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = getUniqueID(context);
        String TOPIC = "/topics/" + prefs.getString("UID", "");

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

    public static void sendNotification(JSONObject notification, String PackageName, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        final String FCM_API = "https://fcm.googleapis.com/fcm/send";
        final String serverKey = "key=AAAARkkdxoQ:APA91bFH_JU9abB0B7OJT-fW0rVjDac-ny13ifdjLU9VqFPp0akohPNVZvfo6mBTFBddcsbgo-pFvtYEyQ62Ohb_arw1GjEqEl4Krc7InJXTxyGqPUkz-VwgTsGzP8Gv_5ZfuqICk7S2";
        final String contentType = "application/json";
        final String TAG = "NOTIFICATION TAG";
        PowerUtils manager = PowerUtils.getInstance(context);
        manager.acquire();

        try {
            String rawPassword = prefs.getString("EncryptionPassword", "");
            JSONObject data = notification.getJSONObject("data");
            if (prefs.getBoolean("UseDataEncryption", false) && !rawPassword.equals("")) {
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
            if (BuildConfig.DEBUG) e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, FCM_API, notification,
                response -> {
                    Log.i(TAG, "onResponse: " + response.toString() + " ,package: " + PackageName);
                    manager.release();
                },
                error -> {
                    Toast.makeText(context, "Failed to send Notification! Please check internet and try again!", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onErrorResponse: Didn't work" + " ,package: " + PackageName);
                    manager.release();
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
}
