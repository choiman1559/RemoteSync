package com.sync.lib.task;

import org.json.JSONObject;

import java.util.ArrayList;

public class RequestTask {
    public final TaskListener listener;
    public final ArrayList<JSONObject> onSuccessQueue;
    public final ArrayList<Exception> onErrorQueue;

    public RequestTask() {
        this.listener = new TaskListener();
        this.onSuccessQueue = new ArrayList<>();
        this.onErrorQueue = new ArrayList<>();
    }

    public void onSuccess(JSONObject response) {
        if(listener.m_onSuccessListener == null) {
            onSuccessQueue.add(response);
        } else {
            listener.m_onSuccessListener.onSuccess(response);
        }
    }

    public void onError(Exception error) {
        if(listener.m_onErrorListener == null) {
            onErrorQueue.add(error);
        } else {
            listener.m_onErrorListener.onError(error);
        }
    }

    public RequestTask setOnSuccessListener(TaskListener.onSuccessListener onSuccessListener) {
        listener.m_onSuccessListener = onSuccessListener;
        for(JSONObject response : onSuccessQueue) {
            listener.m_onSuccessListener.onSuccess(response);
        }

        return this;
    }

    public RequestTask setOnErrorListener(TaskListener.onErrorListener onErrorListener) {
        listener.m_onErrorListener = onErrorListener;
        for(Exception error : onErrorQueue) {
            listener.m_onErrorListener.onError(error);
        }

        return this;
    }
}
