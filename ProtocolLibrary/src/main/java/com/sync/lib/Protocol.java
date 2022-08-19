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

    public static void initialize(Context context, PairAction object) {
        isFindingDeviceToPair = false;
        isListeningToPair = false;
        pairingProcessList = new ArrayList<>();
        pairPrefs = context.getSharedPreferences("com.sync.protocol_pair", MODE_PRIVATE);
        if(connectionOption == null) connectionOption = new ConnectionOption();
        applicationContext = context;
        action = object;
    }

    public static void setConnectionOption(ConnectionOption connectionOption) {
        Protocol.connectionOption = connectionOption;
    }

    public static ConnectionOption getConnectionOption() {
        return connectionOption;
    }

    public static ArrayList<PairDeviceInfo> getPairingProcessList() {
        return pairingProcessList;
    }

    @SuppressWarnings("unchecked")
    public static void onMessageReceived(@NonNull Map<String, String> map) {
        if ("true".equals(map.get("encrypted"))) {
            if (connectionOption.isEncryptionEnabled() && !connectionOption.getEncryptionPassword().equals("")) {
                try {
                    String hashKey = "true".equals(map.get("isFirstFetch")) ? Protocol.connectionOption.getPairingKey() : Protocol.connectionOption.getIdentifierValue();
                    JSONObject object = new JSONObject(AESCrypto.decrypt(CompressStringUtil.decompressString(map.get("encryptedData")), connectionOption.getEncryptionPassword(), hashKey));
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
