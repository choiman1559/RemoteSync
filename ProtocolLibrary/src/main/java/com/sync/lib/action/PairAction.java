package com.sync.lib.action;

import android.content.Context;

import com.sync.lib.data.Data;
import com.sync.lib.data.PairDeviceInfo;

public abstract class PairAction {
    public void onFindRequest() {}
    public void onActionRequested(Data map, Context context) {}
    public void onDataRequested(Data map, Context context) {}
    public void showPairChoiceAction(Data map, Context context) {}
    public void onPairRemoved(PairDeviceInfo device) {}
}
