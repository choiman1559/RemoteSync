package com.sync.lib.util;

import android.content.Context;

import com.sync.lib.Protocol;
import com.sync.lib.data.KeySpec;
import com.sync.lib.data.PairDeviceInfo;
import com.sync.lib.data.Value;
import com.sync.lib.task.RequestTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.Date;

public class DataUtils {
    /**
     * Process and encrypt the json data lastly before send data to push server
     * Once processing is done, the data will be send to push server automatically
     *
     * @param notification Json data to send push server
     * @param PackageName  Current working app's package name
     * @param context      current Android context instance
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask sendNotification(JSONObject notification, String PackageName, Context context) {
        return sendNotification(notification, PackageName, context, false);
    }

    /**
     * Process and encrypt the json data lastly before send data to push server
     * Once processing is done, the data will be send to push server automatically
     *
     * @param notificationBody Json data to send push server
     * @param PackageName      Current working app's package name
     * @param context          current Android context instance
     * @param isFirstFetch     Whether or not you are pitching with the target device for the first time
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask sendNotification(JSONObject notificationBody, String PackageName, Context context, boolean isFirstFetch) {
        Protocol instance = Protocol.getInstance();
        RequestTask task = new RequestTask();

        try {
            KeySpec keySpec = instance.connectionOption.getKeySpec();
            if (instance.connectionOption.isEncryptionEnabled() && keySpec.isValidKey()) {
                if (keySpec.isAuthWithHMac())
                    keySpec.setSecondaryPassword(isFirstFetch ? instance.connectionOption.getPairingKey() : notificationBody.getString(Value.SEND_DEVICE_ID.id()));
                String encryptedData = Crypto.encrypt(notificationBody.toString(), keySpec);

                JSONObject newData = new JSONObject();
                newData.put(Value.ENCRYPTED.id(), "true");
                newData.put(Value.IS_FIRST_FETCHED.id(), String.valueOf(isFirstFetch));
                if (!isFirstFetch)
                    newData.put(Value.SEND_DEVICE_NAME.id(), notificationBody.get(Value.SEND_DEVICE_NAME.id()));
                notificationBody = newData.put(Value.ENCRYPTED_DATA.id(), CompressStringUtil.compressString(encryptedData));
            } else if (!instance.connectionOption.isEncryptionEnabled()) {
                notificationBody = notificationBody.put(Value.ENCRYPTED.id(), "false");
            } else {
                task.onError(new GeneralSecurityException("KeySpec is not valid!"));
                return task;
            }
        } catch (Exception e) {
            task.onError(e);
            return task;
        }

        JSONObject notification = new JSONObject();
        try {
            String Topic = "/topics/" + instance.connectionOption.getPairingKey();
            notification.put(Value.TOPIC.id(), Topic);
            notification.put(Value.PRIORITY.id(), "high");
            notification.put(Value.DATA.id(), notificationBody);
        } catch (JSONException e) {
            task.onError(e);
            return task;
        }

        instance.connectionOption.getRequestInvoker().requestJsonPost(PackageName, context, notification, task);
        return task;
    }

    /**
     * request find task to target device
     *
     * @param device  target device to send
     * @param context current Android context instance
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask sendFindTaskNotification(Context context, PairDeviceInfo device) {
        Protocol instance = Protocol.getInstance();
        Date date = Calendar.getInstance().getTime();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|find");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.SENT_DATE.id(), date);
        } catch (JSONException e) {
            return getErrorResult(e);
        }

        return DataUtils.sendNotification(notificationBody, context.getPackageName(), context);
    }

    /**
     * request some data to target device
     *
     * @param device   target device to send
     * @param context  current Android context instance
     * @param dataType type of data to request
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask requestData(Context context, PairDeviceInfo device, String dataType) {
        Protocol instance = Protocol.getInstance();
        Date date = Calendar.getInstance().getTime();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_data");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.REQUEST_DATA.id(), dataType);
            notificationBody.put(Value.SENT_DATE.id(), date);
        } catch (JSONException e) {
            return getErrorResult(e);
        }
        return DataUtils.sendNotification(notificationBody, context.getPackageName(), context);
    }

    /**
     * response to the device requesting the data
     *
     * @param device      target device to send
     * @param context     current Android context instance
     * @param dataType    type of data requested
     * @param dataContent the value of the requested data
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask responseDataRequest(PairDeviceInfo device, String dataType, String dataContent, Context context) {
        Protocol instance = Protocol.getInstance();
        Date date = Calendar.getInstance().getTime();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|receive_data");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.RECEIVE_DATA.id(), dataContent);
            notificationBody.put(Value.REQUEST_DATA.id(), dataType);
            notificationBody.put(Value.SENT_DATE.id(), date);
        } catch (JSONException e) {
            return getErrorResult(e);
        }
        return DataUtils.sendNotification(notificationBody, context.getPackageName(), context);
    }

    /**
     * request some action to target device
     *
     * @param device   target device to send
     * @param context  current Android context instance
     * @param dataType type of data requested
     * @param args     Argument data required to execute the action
     * @return A RequestTask object to register a task completion listener
     */
    public static RequestTask requestAction(Context context, PairDeviceInfo device, String dataType, String... args) {
        Protocol instance = Protocol.getInstance();
        StringBuilder dataToSend = new StringBuilder();
        if (args.length > 1) {
            for (String str : args) {
                dataToSend.append(str).append("|");
            }
            dataToSend.setCharAt(dataToSend.length() - 1, '\0');
        } else if (args.length == 1) dataToSend.append(args[0]);

        Date date = Calendar.getInstance().getTime();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put(Value.TYPE.id(), "pair|request_action");
            notificationBody.put(Value.DEVICE_NAME.id(), instance.thisDevice.getDevice_name());
            notificationBody.put(Value.DEVICE_ID.id(), instance.thisDevice.getDevice_id());
            notificationBody.put(Value.SEND_DEVICE_NAME.id(), device.getDevice_name());
            notificationBody.put(Value.SEND_DEVICE_ID.id(), device.getDevice_id());
            notificationBody.put(Value.REQUEST_ACTION.id(), dataType);
            notificationBody.put(Value.SENT_DATE.id(), date);
            if (args.length > 0)
                notificationBody.put(Value.ACTION_ARGS.id(), dataToSend.toString());
        } catch (JSONException e) {
            return getErrorResult(e);
        }
        return DataUtils.sendNotification(notificationBody, context.getPackageName(), context);
    }

    public static RequestTask getErrorResult(Exception e) {
        RequestTask task = new RequestTask();
        task.onError(e);
        return task;
    }
}
