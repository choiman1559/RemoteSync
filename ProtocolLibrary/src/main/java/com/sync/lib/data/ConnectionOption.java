package com.sync.lib.data;

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

    public ConnectionOption() {
        encryptionEnabled = false;
        printDebugLog = false;
        showAlreadyConnected = false;
        isReceiveFindRequest = false;
        allowAcceptPairAutomatically = false;
        pairingKey = "";
        identifierValue = "";
        serverKey = "";
        keySpec = new KeySpec();
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
        this.keySpec = keySpec;
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

    public boolean isAllowRemovePairRemotely() {
        return allowRemovePairRemotely;
    }

    public boolean isAllowAcceptPairAutomatically() {
        return allowAcceptPairAutomatically;
    }
}
