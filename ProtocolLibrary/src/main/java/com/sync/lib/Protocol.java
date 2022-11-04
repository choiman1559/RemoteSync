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
import com.sync.lib.util.DataReadWriter;

import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.pushy.sdk.lib.jackson.databind.ObjectMapper;

@SuppressWarnings("unused")
public class Protocol {
    private static Protocol instance;

    public boolean isFindingDeviceToPair;
    public boolean isListeningToPair;
    public ArrayList<PairDeviceInfo> pairingProcessList;
    public ConnectionOption connectionOption;
    public SharedPreferences pairPrefs;
    public Context applicationContext;
    public PairAction action;
    public PairDeviceInfo thisDevice;

    static SharedPreferences.OnSharedPreferenceChangeListener onChange = ((sharedPreferences, key) -> {
        if(instance != null && key.equals(DataReadWriter.DEFAULT_DATASET_KEY)) {
            PairListener.m_onDeviceListChangedListener.onReceive(instance.getPairedDeviceList());
        }
    });

    /**
     * get previously initialized Protocol instance
     * This method must be called after calling initialize() method at least once
     *
     * @return an Protocol instance
     * @throws NullPointerException throws when previously initialized Protocol instance is not available
     */
    public synchronized static Protocol getInstance() throws NullPointerException {
        if(instance == null) throw new NullPointerException("Protocol is not initialized yet!");
        else return instance;
    }

    /**
     * initialize protocol
     * This method must be called on Application startup before use the protocol
     *
     * @param context Android application class context
     * @param object Custom class that extends PairAction Listener to get action requested from protocol
     * @return an initialized Protocol instance
     */
    public synchronized static Protocol initialize(Context context, PairAction object) {
        instance = new Protocol();

        instance.isFindingDeviceToPair = false;
        instance.isListeningToPair = false;
        instance.pairingProcessList = new ArrayList<>();
        instance.pairPrefs = context.getSharedPreferences("com.sync.protocol_pair", MODE_PRIVATE);
        if(instance.connectionOption == null) instance.connectionOption = new ConnectionOption();
        instance.applicationContext = context;
        instance.action = object;
        instance.thisDevice = new PairDeviceInfo(Build.MANUFACTURER + " " + Build.MODEL, instance.connectionOption.getIdentifierValue());
        instance.pairPrefs.registerOnSharedPreferenceChangeListener(onChange);

        return instance;
    }

    /**
     * apply new connection option
     *
     * @param connectionOption user-customized connection option
     */
    public void setConnectionOption(ConnectionOption connectionOption) {
        this.connectionOption = connectionOption;
        this.thisDevice = new PairDeviceInfo(Build.MANUFACTURER + " " + Build.MODEL, connectionOption.getIdentifierValue());
    }

    /**
     * get current connection option
     * @return current ConnectionOption object
     */
    public ConnectionOption getConnectionOption() {
        return this.connectionOption;
    }

    /**
     * get list of current pairing devices
     * @return current ArrayList<PairDeviceInfo> object
     */
    public ArrayList<PairDeviceInfo> getPairingProcessList() {
        return this.pairingProcessList;
    }

    /**
     * get list of current paired devices
     * @return current ArrayList<PairDeviceInfo> object
     */
    public ArrayList<PairDeviceInfo> getPairedDeviceList() {
        Set<String> array = connectionOption.getDataReadWriter().readData(DataReadWriter.DEFAULT_DATASET_KEY);
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
    public void onMessageReceived(@NonNull Map<String, String> rawMap) {
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

                    if(keySpec.isAuthWithHMac()) keySpec.setSecondaryPassword(isFirstFetch ? connectionOption.getPairingKey() : keySpec.getSecondaryPassword());
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
