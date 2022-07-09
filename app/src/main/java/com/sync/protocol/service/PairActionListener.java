package com.sync.protocol.service;

import android.content.Context;

import com.sync.lib.action.PairAction;

import java.util.Map;

public class PairActionListener extends PairAction {
    @Override
    public void onFindRequest() {
        FirebaseMessageService.getInstance().sendFindTaskNotification();
    }

    @Override
    public void onActionRequested(Map<String, String> map, Context context) {
        DataProcess.onActionRequested(map, context);
    }

    @Override
    public void onDataRequested(Map<String, String> map, Context context) {
        DataProcess.onDataRequested(map, context);
    }

    @Override
    public void showPairChoiceAction(Map<String, String> map, Context context) {
        DataProcess.showPairChoiceAction(map, context);
    }
}
