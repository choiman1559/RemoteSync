package com.sync.lib.task;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.sync.lib.Protocol;
import com.sync.lib.util.JsonRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class RequestInvoker {
    /**
     * send json data to actual push server
     * You can use custom push provider by extend this class
     *
     * @param notification Json data to send push server
     * @param PackageName  Current working app's package name
     * @param context      Current Android context instance
     * @param task         An object for calling the job completion listener
     */
    public void requestJsonPost(String PackageName, Context context, JSONObject notification, RequestTask task) {
        Protocol instance = Protocol.getInstance();
        final String FCM_API = "https://fcm.googleapis.com/fcm/send";
        final String serverKey = instance.connectionOption.getServerKey();
        final String contentType = "application/json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, FCM_API, notification, task::onSuccess, task::onError) {
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
