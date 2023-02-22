package com.sync.protocol.service;

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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.sync.lib.Protocol;
import com.sync.protocol.BuildConfig;
import com.sync.protocol.R;
import com.sync.protocol.utils.PowerUtils;

public class FirebaseMessageService extends FirebaseMessagingService {

    SharedPreferences prefs;
    private static PowerUtils manager;
    public static volatile Ringtone lastPlayedRingtone;
    public static final Thread ringtonePlayedThread = new Thread(() -> {
        while (true) {
            if (lastPlayedRingtone != null && !lastPlayedRingtone.isPlaying())
                lastPlayedRingtone.play();
        }
    });
    static FirebaseMessageService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("com.sync.protocol_preferences", MODE_PRIVATE);
        manager = PowerUtils.getInstance(this);
        manager.acquire();
        instance = this;
    }

    public static FirebaseMessageService getInstance() {
        return instance;
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        manager.acquire();
        if (BuildConfig.DEBUG) Log.d(remoteMessage.getMessageId(), remoteMessage.toString());

        if (prefs.getBoolean("ServiceToggle", false)) {
            Protocol.getInstance().onMessageReceived(remoteMessage.getData());
        }
    }

    public void sendFindTaskNotification() {
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

        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(true);

        if (lastPlayedRingtone != null && lastPlayedRingtone.isPlaying()) lastPlayedRingtone.stop();
        lastPlayedRingtone = RingtoneManager.getRingtone(this, RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE));
        if (Build.VERSION.SDK_INT >= 28) {
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