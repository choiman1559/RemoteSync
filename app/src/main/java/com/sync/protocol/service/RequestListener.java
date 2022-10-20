package com.sync.protocol.service;

import android.content.Context;

import com.sync.lib.task.RequestInvoker;
import com.sync.lib.task.RequestTask;

import org.json.JSONObject;

public class RequestListener extends RequestInvoker {
    @Override
    public void requestJsonPost(String PackageName, Context context, JSONObject notification, RequestTask task) {
        super.requestJsonPost(PackageName, context, notification, task);
    }
}
