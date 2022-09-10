package com.sync.lib.util;

import android.annotation.SuppressLint;
import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class JsonRequest {
    @SuppressLint("StaticFieldLeak")
    private static JsonRequest instance;
    private RequestQueue requestQueue;
    private final Context ctx;

    private JsonRequest(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
    }

    /**
     * initialize class if instance is not available, then return instance
     *
     * @param context Android application class context
     * @return A JsonRequest instance.
     */
    protected static synchronized JsonRequest getInstance(Context context) {
        if (instance == null) {
            instance = new JsonRequest(context);
        }
        return instance;
    }

    /**
     * Creates a default instance of the worker pool and calls RequestQueue.start() on it.
     *
     * @return A started RequestQueue instance.
     */
    protected RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    /**
     * Adds a Request to the dispatch queue.
     * @param req Json request instance to query
     * @param <T> object type
     */
    protected <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}