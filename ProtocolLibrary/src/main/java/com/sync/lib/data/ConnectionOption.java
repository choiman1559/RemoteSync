package com.sync.lib.data;

import android.content.Context;

import com.sync.lib.task.RequestInvoker;
import com.sync.lib.task.RequestTask;

import org.json.JSONObject;

public class ConnectionOption {
    private String pairingKey;
    private String identifierValue;
    private boolean encryptionEnabled;
    private boolean printDebugLog;
    private boolean showAlreadyConnected;
    private boolean isReceiveFindRequest;
    private boolean allowRemovePairRemotely;
    private boolean allowAcceptPairAutomatically;
    private String serverKey;
    private KeySpec keySpec;
    private RequestInvoker requestInvoker;

    public ConnectionOption() {
        encryptionEnabled = false;
        printDebugLog = false;
        showAlreadyConnected = false;
        isReceiveFindRequest = false;
        allowAcceptPairAutomatically = false;
        pairingKey = "";
        identifierValue = "";
        serverKey = "";
        keySpec = new KeySpec.Builder().build();
        requestInvoker = new RequestInvoker() {
            @Override
            public void requestJsonPost(String PackageName, Context context, JSONObject notification, RequestTask task) {
                super.requestJsonPost(PackageName, context, notification, task);
            }
        };
    }

    public void setServerKey(String serverKey) {
        this.serverKey = serverKey;
    }

    public void setPairingKey(String pairingKey) {
        this.pairingKey = pairingKey;
    }

    public void setDenyFindRequest(boolean receiveFindRequest) {
        isReceiveFindRequest = receiveFindRequest;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public void setPrintDebugLog(boolean printDebugLog) {
        this.printDebugLog = printDebugLog;
    }

    public void setShowAlreadyConnected(boolean showAlreadyConnected) {
        this.showAlreadyConnected = showAlreadyConnected;
    }

    public void setAllowRemovePairRemotely(boolean allowRemovePairRemotely) {
        this.allowRemovePairRemotely = allowRemovePairRemotely;
    }

    public void setAllowAcceptPairAutomatically(boolean allowAcceptPairAutomatically) {
        this.allowAcceptPairAutomatically = allowAcceptPairAutomatically;
    }

    public void setKeySpec(KeySpec keySpec) {
        keySpec.setSecondaryPassword(identifierValue);
        this.keySpec = keySpec;
    }

    public void setRequestInvoker(RequestInvoker requestInvoker) {
        this.requestInvoker = requestInvoker;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public boolean isPrintDebugLog() {
        return printDebugLog;
    }

    public boolean isShowAlreadyConnected() {
        return showAlreadyConnected;
    }

    public KeySpec getKeySpec() {
        return keySpec;
    }

    public String getIdentifierValue() {
        return identifierValue;
    }

    public String getPairingKey() {
        return pairingKey;
    }

    public boolean isReceiveFindRequest() {
        return isReceiveFindRequest;
    }

    public String getServerKey() {
        return serverKey;
    }

    public RequestInvoker getRequestInvoker() {
        return requestInvoker;
    }

    public boolean isAllowRemovePairRemotely() {
        return allowRemovePairRemotely;
    }

    public boolean isAllowAcceptPairAutomatically() {
        return allowAcceptPairAutomatically;
    }
}
