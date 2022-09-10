package com.sync.protocol.service;

import android.content.Context;

import com.sync.lib.action.PairAction;
import com.sync.lib.data.Data;
import com.sync.lib.data.PairDeviceInfo;

import java.util.Map;

public class PairActionListener extends PairAction {
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
        super.onPairRemoved(device);
        //Useless for now: Ignore this event
    }
}
