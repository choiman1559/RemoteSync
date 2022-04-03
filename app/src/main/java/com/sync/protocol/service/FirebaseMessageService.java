package com.sync.protocol.service;

import static com.sync.protocol.Application.pairingProcessList;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.sync.protocol.Application;
import com.sync.protocol.BuildConfig;
import com.sync.protocol.R;
import com.sync.protocol.service.pair.DataProcess;
import com.sync.protocol.service.pair.PairDeviceInfo;
import com.sync.protocol.service.pair.PairDeviceStatus;
import com.sync.protocol.service.pair.PairListener;
import com.sync.protocol.service.pair.PairingUtils;
import com.sync.protocol.utils.AESCrypto;
import com.sync.protocol.utils.CompressStringUtil;
import com.sync.protocol.utils.DataUtils;
import com.sync.protocol.utils.PowerUtils;

import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Map;

import me.pushy.sdk.lib.jackson.databind.ObjectMapper;

public class FirebaseMessageService extends FirebaseMessagingService {
    SharedPreferences prefs;
    SharedPreferences pairPrefs;
    private static PowerUtils manager;
    public static volatile Ringtone lastPlayedRingtone;
    public static final Thread ringtonePlayedThread = new Thread(() -> {
        while(true) {
            if(lastPlayedRingtone != null && !lastPlayedRingtone.isPlaying()) lastPlayedRingtone.play();
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("com.sync.protocol_preferences", MODE_PRIVATE);
        pairPrefs = getSharedPreferences("com.sync.protocol_pair", MODE_PRIVATE);
        manager = PowerUtils.getInstance(this);
        manager.acquire();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        manager.acquire();

        if(BuildConfig.DEBUG) Log.d(remoteMessage.getMessageId(), remoteMessage.toString());
        Map<String,String> map = remoteMessage.getData();

        String rawPassword = prefs.getString("EncryptionPassword", "");
        if("true".equals(map.get("encrypted"))) {
            if(prefs.getBoolean("UseDataEncryption", false) && !rawPassword.equals("")) {
                try {
                    JSONObject object = new JSONObject(AESCrypto.decrypt(CompressStringUtil.decompressString(map.get("encryptedData")), rawPassword));
                    Map<String, String> newMap = new ObjectMapper().readValue(object.toString(), Map.class);
                    processReception(newMap, this);
                } catch (GeneralSecurityException e) {
                    Handler mHandler = new Handler(Looper.getMainLooper());
                    mHandler.postDelayed(() -> Toast.makeText(this, "Error occurred while decrypting data!\nPlease check password and try again!", Toast.LENGTH_SHORT).show(), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else processReception(map, this);
    }

    private void processReception(Map<String, String> map, Context context) {
        String type = map.get("type");
        if(type != null && !prefs.getString("UID", "").equals("")) {
            if(type.startsWith("pair") && !isDeviceItself(map)) {
                switch(type) {
                    case "pair|request_device_list":
                        //Target Device action
                        //Have to Send this device info Data Now
                        if(!isPairedDevice(map) || prefs.getBoolean("showAlreadyConnected", false)) {
                            pairingProcessList.add(new PairDeviceInfo(map.get("device_name"), map.get("device_id"), PairDeviceStatus.Device_Process_Pairing));
                            Application.isListeningToPair = true;
                            PairingUtils.responseDeviceInfoToFinder(map, context);
                        }
                        break;

                    case "pair|response_device_list":
                        //Request Device Action
                        //Show device list here; give choice to user which device to pair
                        if(Application.isFindingDeviceToPair && (!isPairedDevice(map) || prefs.getBoolean("showAlreadyConnected", false))) {
                            pairingProcessList.add(new PairDeviceInfo(map.get("device_name"), map.get("device_id"), PairDeviceStatus.Device_Process_Pairing));
                            PairingUtils.onReceiveDeviceInfo(map);
                        }
                        break;

                    case "pair|request_pair":
                        //Target Device action
                        //Show choice notification (or activity) to user whether user wants to pair this device with another one or not
                        if(Application.isListeningToPair && isTargetDevice(map)) {
                            for(PairDeviceInfo info : pairingProcessList) {
                                if(info.getDevice_name().equals(map.get("device_name")) && info.getDevice_id().equals(map.get("device_id"))) {
                                    PairingUtils.showPairChoiceAction(map, context);
                                    break;
                                }
                            }
                        }
                        break;

                    case "pair|accept_pair":
                        //Request Device Action
                        //Check if target accepted to pair and process result here
                        if(Application.isFindingDeviceToPair && isTargetDevice(map)) {
                            for(PairDeviceInfo info : pairingProcessList) {
                                if (info.getDevice_name().equals(map.get("device_name")) && info.getDevice_id().equals(map.get("device_id"))) {
                                    PairingUtils.checkPairResultAndRegister(map, info, context);
                                    break;
                                }
                            }
                        }
                        break;

                    case "pair|request_data":
                        //process request normal data here sent by paired device(s).
                        if(isTargetDevice(map) && isPairedDevice(map)) {
                            DataProcess.onDataRequested(map, context);
                        }
                        break;

                    case "pair|receive_data":
                        //process received normal data here sent by paired device(s).
                        if(isTargetDevice(map) && isPairedDevice(map)) {
                            PairListener.callOnDataReceived(map);
                        }
                        break;

                    case "pair|request_action":
                        //process received action data here sent by paired device(s).
                        if(isTargetDevice(map) && isPairedDevice(map)) {
                            DataProcess.onActionRequested(map, context);
                        }
                        break;

                    case "pair|find":
                        if(isTargetDevice(map) && isPairedDevice(map) && !prefs.getBoolean("NotReceiveFindDevice", false)) {
                            sendFindTaskNotification();
                        }
                        break;
                }
            }
        }
    }

    protected boolean isDeviceItself(Map<String, String> map) {
        String Device_name = map.get("device_name");
        String Device_id = map.get("device_id");

        if(Device_id == null || Device_name == null) {
            Device_id = map.get("send_device_id");
            Device_name = map.get("send_device_name");
        }

        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = DataUtils.getUniqueID(this);

        return DEVICE_NAME.equals(Device_name) && DEVICE_ID.equals(Device_id);
    }

    protected boolean isTargetDevice(Map<String, String> map) {
        String Device_name = map.get("send_device_name");
        String Device_id = map.get("send_device_id");

        String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;
        String DEVICE_ID = DataUtils.getUniqueID(this);

        return DEVICE_NAME.equals(Device_name) && DEVICE_ID.equals(Device_id);
    }

    protected boolean isPairedDevice(Map<String, String> map) {
        String dataToFind = map.get("device_name") + "|" + map.get("device_id");
        for(String str : pairPrefs.getStringSet("paired_list", new HashSet<>())) {
            if(str.equals(dataToFind)) return true;
        }
        return false;
    }

    protected void sendFindTaskNotification() {
        manager.acquire();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(FirebaseMessageService.this, -2,
                new Intent(FirebaseMessageService.this, FindDeviceCancelReceiver.class),
                Build.VERSION.SDK_INT > 30 ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.notify_channel_id))
                .setContentTitle("Finding my devices...")
                .setContentText("User requested playing sound\nto find the device!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup(getPackageName() + ".NOTIFICATION")
                .setGroupSummary(false)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_info_outline_black_24dp, "Stop", pendingIntent);

        assert notificationManager != null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setSmallIcon(R.drawable.ic_notification);
            CharSequence channelName = getString(R.string.notify_channel_name);
            String description = getString(R.string.notify_channel_description);

            NotificationChannel channel = new NotificationChannel(getString(R.string.notify_channel_id), channelName, getImportance());
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        } else builder.setSmallIcon(R.mipmap.ic_notification);

        final int findDeviceNotificationId = -2;
        notificationManager.notify(findDeviceNotificationId, builder.build());

        AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(true);

        if(lastPlayedRingtone != null && lastPlayedRingtone.isPlaying()) lastPlayedRingtone.stop();
        lastPlayedRingtone = RingtoneManager.getRingtone(this, RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE));
        if(Build.VERSION.SDK_INT >= 28) {
            AudioAttributes.Builder audioAttributes = new AudioAttributes.Builder();
            audioAttributes.setUsage(AudioAttributes.USAGE_ALARM);
            audioAttributes.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC);

            lastPlayedRingtone.setLooping(true);
            lastPlayedRingtone.setAudioAttributes(audioAttributes.build());
            lastPlayedRingtone.play();
        } else ringtonePlayedThread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private int getImportance() {
        String value = prefs.getString("importance", "Default");
        switch (value) {
            case "Default":
                return NotificationManager.IMPORTANCE_DEFAULT;
            case "Low":
                return NotificationManager.IMPORTANCE_LOW;
            case "High":
                return NotificationManager.IMPORTANCE_MAX;
            case "Custom…":
                return NotificationManager.IMPORTANCE_NONE;
            default:
                return NotificationManager.IMPORTANCE_UNSPECIFIED;
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        if (!prefs.getString("UID", "").equals(""))
            FirebaseMessaging.getInstance().subscribeToTopic(prefs.getString("UID", ""));
    }
}