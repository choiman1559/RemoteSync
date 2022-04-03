package com.sync.protocol;

import com.kieronquinn.monetcompat.core.MonetCompat;
import com.sync.protocol.service.pair.PairDeviceInfo;

import java.util.ArrayList;

public class Application extends android.app.Application {
    public static boolean isFindingDeviceToPair = false;
    public static boolean isListeningToPair = false;
    public static ArrayList<PairDeviceInfo> pairingProcessList = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        MonetCompat.enablePaletteCompat();
    }
}