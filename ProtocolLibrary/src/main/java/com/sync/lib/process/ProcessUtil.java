package com.sync.lib.process;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.sync.lib.Protocol;
import com.sync.lib.action.PairListener;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.data.PairDeviceStatus;

import java.util.HashSet;
import java.util.Map;

public class ProcessUtil {
    /**
     * process incoming data
     *
     * @param context Android application class context
     * @param map Custom class that extends PairAction Listener to get action requested from protocol
     */
    public static void processReception(Map<String, String> map, Context context) {
        String type = map.get("type");
        if(Protocol.connectionOption.isPrintDebugLog()) Log.d("SyncProtocol", type + "Sent device: " + map.get("device_name") + map.get("device_id"));
        if (type != null && !Protocol.connectionOption.getPairingKey().equals("")) {
            if (type.startsWith("pair") && !isDeviceItself(map)) {
                PairDeviceInfo device = new PairDeviceInfo(map.get("device_name"), map.get("device_id"), PairDeviceStatus.Device_Process_Pairing);

                switch (type) {
                    case "pair|request_device_list":
                        //Target Device action
                        //Have to Send this device info Data Now
                        if (!isPairedDevice(map) || Protocol.connectionOption.isShowAlreadyConnected()) {
                            Protocol.pairingProcessList.add(device);
                            Protocol.isListeningToPair = true;
                            Process.responseDeviceInfoToFinder(map, context);
                        }
                        break;

                    case "pair|response_device_list":
                        //Request Device Action
                        //Show device list here; give choice to user which device to pair
                        if (Protocol.isFindingDeviceToPair && (!isPairedDevice(map) || Protocol.connectionOption.isShowAlreadyConnected())) {
                            Protocol.pairingProcessList.add(device);
                            Process.onReceiveDeviceInfo(map);
                        }
                        break;

                    case "pair|request_pair":
                        //Target Device action
                        //Show choice notification (or activity) to user whether user wants to pair this device with another one or not
                        if (Protocol.isListeningToPair && isTargetDevice(map)) {
                            for (PairDeviceInfo info : Protocol.pairingProcessList) {
                                if (info.equals(device)) {
                                    if(Protocol.connectionOption.isAllowRemovePairRemotely()) {
                                        Process.registerDevice(device);
                                    } else {
                                        Protocol.action.showPairChoiceAction(map, context);
                                    }
                                    break;
                                }
                            }
                        }
                        break;

                    case "pair|accept_pair":
                        //Request Device Action
                        //Check if target accepted to pair and process result here
                        if (Protocol.isFindingDeviceToPair && isTargetDevice(map)) {
                            for (PairDeviceInfo info : Protocol.pairingProcessList) {
                                if (info.getDevice_name().equals(map.get("device_name")) && info.getDevice_id().equals(map.get("device_id"))) {
                                    Process.checkPairResultAndRegister(map, info, context);
                                    break;
                                }
                            }
                        }
                        break;

                    case "pair|request_remove":
                        if(isTargetDevice(map) && isPairedDevice(map) && Protocol.connectionOption.isAllowRemovePairRemotely()) {
                            Process.removePairedDevice(map);
                        }
                        break;

                    case "pair|request_data":
                        //process request normal data here sent by paired device(s).
                        if (isTargetDevice(map) && isPairedDevice(map)) {
                            Protocol.action.onDataRequested(map, context);
                        }
                        break;

                    case "pair|receive_data":
                        //process received normal data here sent by paired device(s).
                        if (isTargetDevice(map) && isPairedDevice(map)) {
                            PairListener.callOnDataReceived(map);
                        }
                        break;

                    case "pair|request_action":
                        //process received action data here sent by paired device(s).
                        if (isTargetDevice(map) && isPairedDevice(map)) {
                            Protocol.action.onActionRequested(map, context);
                        }
                        break;

                    case "pair|find":
                        if (isTargetDevice(map) && isPairedDevice(map) && !Protocol.connectionOption.isReceiveFindRequest()) {
                            Protocol.action.onFindRequest();
                        }
                        break;
                }
            }
        }
    }

    /**
     * Check that the device that sent the data and the device that received the data are the same device
     *
     * @param map raw data received from push server
     */
    protected static boolean isDeviceItself(Map<String, String> map) {
        String Device_name = map.get("device_name");
        String Device_id = map.get("device_id");

        if (Device_id == null || Device_name == null) {
            Device_id = map.get("send_device_id");
            Device_name = map.get("send_device_name");
        }

        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = Protocol.connectionOption.getIdentifierValue();

        return DEVICE_NAME.equals(Device_name) && DEVICE_ID.equals(Device_id);
    }

    /**
     * Check if the target of the data is this device
     *
     * @param map raw data received from push server
     */
    protected static boolean isTargetDevice(Map<String, String> map) {
        String Device_name = map.get("send_device_name");
        String Device_id = map.get("send_device_id");

        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = Protocol.connectionOption.getIdentifierValue();

        return DEVICE_NAME.equals(Device_name) && DEVICE_ID.equals(Device_id);
    }

    /**
     * Check if the device that sent this data is paired
     *
     * @param map raw data received from push server
     */
    protected static boolean isPairedDevice(Map<String, String> map) {
        String dataToFind = map.get("device_name") + "|" + map.get("device_id");
        for (String str : Protocol.pairPrefs.getStringSet("paired_list", new HashSet<>())) {
            if (str.equals(dataToFind)) return true;
        }
        return false;
    }
}
