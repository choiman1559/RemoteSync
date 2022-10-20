package com.sync.lib.task;

import org.json.JSONObject;

final public class TaskListener {
    volatile onSuccessListener m_onSuccessListener;
    volatile onErrorListener m_onErrorListener;

    public interface onSuccessListener {
        void onSuccess(JSONObject response);
    }

    public interface onErrorListener {
        void onError(Exception error);
    }
}
