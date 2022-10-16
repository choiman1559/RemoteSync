package com.sync.protocol.service;

import android.content.Context;

import com.sync.lib.action.PairAction;
import com.sync.lib.data.Data;
import com.sync.lib.data.PairDeviceInfo;

public class PairActionListener implements PairAction {
    @Override
    public void onFindRequest() {
        FirebaseMessageService.getInstance().sendFindTaskNotification();
    }

    @Override
    public void onActionRequested(Data map, Context context) {
        DataProcess.onActionRequested(map, context);
    }

    @Override
    public void onDataRequested(Data map, Context context) {
        DataProcess.onDataRequested(map, context);
    }

    @Override
    public void showPairChoiceAction(Data map, Context context) {
        DataProcess.showPairChoiceAction(map, context);
    }

    @Override
    public void onPairRemoved(PairDeviceInfo device) {
        //Useless for now: Ignore this event
    }
}
