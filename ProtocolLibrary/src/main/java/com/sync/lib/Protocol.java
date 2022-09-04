package com.sync.lib;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sync.lib.action.PairAction;
import com.sync.lib.data.ConnectionOption;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.process.ProcessUtil;
import com.sync.lib.util.AESCrypto;
import com.sync.lib.util.CompressStringUtil;

import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Map;

import me.pushy.sdk.lib.jackson.databind.ObjectMapper;

public class Protocol {
    public static boolean isFindingDeviceToPair;
    public static boolean isListeningToPair;
    public static ArrayList<PairDeviceInfo> pairingProcessList;
    public static ConnectionOption connectionOption;
    public static SharedPreferences pairPrefs;
    public static Context applicationContext;
    public static PairAction action;

    /**
     * initialize protocol
     * This method must be called on Application startup before use the protocol
     *
     * @param context Android application class context
     * @param object Custom class that extends PairAction Listener to get action requested from protocol
     */
    public static void initialize(Context context, PairAction object) {
        isFindingDeviceToPair = false;
        isListeningToPair = false;
        pairingProcessList = new ArrayList<>();
        pairPrefs = context.getSharedPreferences("com.sync.protocol_pair", MODE_PRIVATE);
        if(connectionOption == null) connectionOption = new ConnectionOption();
        applicationContext = context;
        action = object;
    }

    /**
     * apply new connection option
     *
     * @param connectionOption user-customized connection option
     */
    public static void setConnectionOption(ConnectionOption connectionOption) {
        Protocol.connectionOption = connectionOption;
    }

    /**
     * get current connection option
     * @return current ConnectionOption object
     */
    public static ConnectionOption getConnectionOption() {
        return connectionOption;
    }

    /**
     * get list of current pairing devices
     * @return current ArrayList<PairDeviceInfo> object
     */
    public static ArrayList<PairDeviceInfo> getPairingProcessList() {
        return pairingProcessList;
    }

    /**
     * When the data received from FCM is passed to this function, the protocol starts processing
     *
     * @param map Raw data from FCM
     */
    @SuppressWarnings("unchecked")
    public static void onMessageReceived(@NonNull Map<String, String> map) {
        if ("true".equals(map.get("encrypted"))) {
            if (connectionOption.isEncryptionEnabled() && !connectionOption.getEncryptionPassword().equals("")) {
                try {
                    JSONObject object;
                    if(connectionOption.isAuthWithHMac()) {
                        String hashKey = "true".equals(map.get("isFirstFetch")) ? Protocol.connectionOption.getPairingKey() : Protocol.connectionOption.getIdentifierValue();
                        object = new JSONObject(AESCrypto.decrypt(CompressStringUtil.decompressString(map.get("encryptedData")), connectionOption.getEncryptionPassword(), hashKey));
                    } else {
                        object = new JSONObject(AESCrypto.decrypt(CompressStringUtil.decompressString(map.get("encryptedData")), connectionOption.getEncryptionPassword()));
                    }

                    Map<String, String> newMap = new ObjectMapper().readValue(object.toString(), Map.class);
                    ProcessUtil.processReception(newMap, applicationContext);
                } catch (GeneralSecurityException e) {
                    Log.e("SyncProtocol", "Error occurred while decrypting data!\nCause: " + e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else ProcessUtil.processReception(map, applicationContext);
    }
}
