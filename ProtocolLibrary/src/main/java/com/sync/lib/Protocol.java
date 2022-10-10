package com.sync.lib;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sync.lib.action.PairAction;
import com.sync.lib.action.PairListener;
import com.sync.lib.data.ConnectionOption;
import com.sync.lib.data.Data;
import com.sync.lib.data.KeySpec;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.data.Value;
import com.sync.lib.process.ProcessUtil;
import com.sync.lib.util.Crypto;
import com.sync.lib.util.CompressStringUtil;

import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.pushy.sdk.lib.jackson.databind.ObjectMapper;

public class Protocol {
    public static boolean isFindingDeviceToPair;
    public static boolean isListeningToPair;
    public static ArrayList<PairDeviceInfo> pairingProcessList;
    public static ConnectionOption connectionOption;
    public static SharedPreferences pairPrefs;
    public static Context applicationContext;
    public static PairAction action;
    public static PairDeviceInfo thisDevice;

    static SharedPreferences.OnSharedPreferenceChangeListener onChange = ((sharedPreferences, key) -> {
        if(key.equals("paired_list")) {
            PairListener.m_onDeviceListChangedListener.onReceive(getPairedDeviceList());
        }
    });

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
        thisDevice = new PairDeviceInfo(Build.MANUFACTURER + " " + Build.MODEL, connectionOption.getIdentifierValue());
        pairPrefs.registerOnSharedPreferenceChangeListener(onChange);
    }

    /**
     * apply new connection option
     *
     * @param connectionOption user-customized connection option
     */
    public static void setConnectionOption(ConnectionOption connectionOption) {
        Protocol.connectionOption = connectionOption;
        thisDevice = new PairDeviceInfo(Build.MANUFACTURER + " " + Build.MODEL, connectionOption.getIdentifierValue());
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
     * get list of current paired devices
     * @return current ArrayList<PairDeviceInfo> object
     */
    public static ArrayList<PairDeviceInfo> getPairedDeviceList() {
        Set<String> array = pairPrefs.getStringSet("paired_list", new HashSet<>());
        ArrayList<PairDeviceInfo> list = new ArrayList<>();
        for(String str : array) {
            String[] data = str.split("\\|");
            list.add(new PairDeviceInfo(data[0], data[1]));
        }
        return list;
    }

    /**
     * When the data received from FCM is passed to this function, the protocol starts processing
     *
     * @param rawMap Raw data from FCM
     */
    @SuppressWarnings("unchecked")
    public static void onMessageReceived(@NonNull Map<String, String> rawMap) {
        Data map = new Data(rawMap);
        if ("true".equals(map.get(Value.ENCRYPTED))) {
            KeySpec keySpec = connectionOption.getKeySpec();
            if (connectionOption.isEncryptionEnabled() && keySpec.isValidKey()) {
                try {
                    JSONObject object;
                    boolean isFirstFetch = "true".equals(map.get(Value.IS_FIRST_FETCHED));
                    if(!isFirstFetch && !thisDevice.getDevice_name().equals(map.get(Value.SEND_DEVICE_NAME))) {
                        return;
                    }

                    if(keySpec.isAuthWithHMac()) keySpec.setSecondaryPassword(isFirstFetch ? Protocol.connectionOption.getPairingKey() : keySpec.getSecondaryPassword());
                    object = new JSONObject(Crypto.decrypt(CompressStringUtil.decompressString(map.get(Value.ENCRYPTED_DATA)), keySpec));

                    Map<String, String> newMap = new ObjectMapper().readValue(object.toString(), Map.class);
                    ProcessUtil.processReception(new Data(newMap), applicationContext);
                } catch (GeneralSecurityException e) {
                    Log.e("SyncProtocol", "Error occurred while decrypting data!\nCause: " + e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else ProcessUtil.processReception(map, applicationContext);
    }
}
