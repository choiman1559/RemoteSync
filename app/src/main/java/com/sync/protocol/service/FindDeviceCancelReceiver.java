package com.sync.protocol.service;

import static com.sync.protocol.service.FirebaseMessageService.lastPlayedRingtone;
import static com.sync.protocol.service.FirebaseMessageService.ringtonePlayedThread;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class FindDeviceCancelReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(lastPlayedRingtone != null && lastPlayedRingtone.isPlaying()) {
            lastPlayedRingtone.stop();

            if(Build.VERSION.SDK_INT >= 28) lastPlayedRingtone.setLooping(false);
            else if(ringtonePlayedThread.isAlive()) ringtonePlayedThread.interrupt();

            lastPlayedRingtone = null;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(-2);
    }
}
