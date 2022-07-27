package com.sync.lib.action;

import android.content.Context;

import java.util.Map;

public class PairAction {
    public void onFindRequest() {}
    public void onActionRequested(Map<String, String> map, Context context) {}
    public void onDataRequested(Map<String, String> map, Context context) {}
    public void showPairChoiceAction(Map<String, String> map, Context context) {}
    public void onPairRemoved(Map<String, String> map) {}
}
