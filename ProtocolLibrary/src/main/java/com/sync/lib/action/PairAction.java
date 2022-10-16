package com.sync.lib.action;

import android.content.Context;

import com.sync.lib.data.Data;
import com.sync.lib.data.PairDeviceInfo;

public interface PairAction {
    void onFindRequest();

    void onActionRequested(Data map, Context context);

    void onDataRequested(Data map, Context context);

    void showPairChoiceAction(Data map, Context context);

    void onPairRemoved(PairDeviceInfo device);
}
