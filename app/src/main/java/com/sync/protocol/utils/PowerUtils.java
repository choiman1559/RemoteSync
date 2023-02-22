package com.sync.protocol.utils;

import android.content.Context;
import android.os.PowerManager;

public class PowerUtils {
    private static final String TAG = PowerUtils.class.getSimpleName();
    private static PowerUtils instance = null;
    private static PowerManager.WakeLock wakeLock;

    private PowerUtils() {
    }

    public static PowerUtils getInstance(Context context) {
        if (instance == null) instance = new PowerUtils();
        if (wakeLock == null) {
            PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        return instance;
    }

    public boolean isHeld() {
        return wakeLock != null && wakeLock.isHeld();
    }

    public void acquire(long timeout) {
        if (!isHeld()) {
            wakeLock.acquire(timeout);
        }
    }

    public void acquire() {
        //10 minutes timeout for battery save
        acquire(10 * 60 * 1000L);
    }

    public void release() {
        if (isHeld()) {
            wakeLock.release();
        }
    }
}