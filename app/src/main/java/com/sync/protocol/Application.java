package com.sync.protocol;

import android.content.SharedPreferences;

import com.kieronquinn.monetcompat.core.MonetCompat;
import com.sync.lib.Protocol;
import com.sync.lib.data.ConnectionOption;
import com.sync.protocol.service.PairActionListener;
import com.sync.protocol.utils.DataUtils;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MonetCompat.enablePaletteCompat();
        SharedPreferences prefs = getSharedPreferences("com.sync.protocol_preferences", MODE_PRIVATE);

        Protocol.initialize(this, new PairActionListener());
        ConnectionOption option = new ConnectionOption();

        option.setPairingKey(prefs.getString("UID", ""));
        option.setIdentifierValue(DataUtils.getUniqueID(this));
        option.setEncryptionEnabled(prefs.getBoolean("UseDataEncryption", false));
        option.setEncryptionPassword(prefs.getString("EncryptionPassword", ""));
        option.setPrintDebugLog(prefs.getBoolean("printDebugLog", false));
        option.setDenyFindRequest(prefs.getBoolean("NotReceiveFindDevice", false));
        option.setShowAlreadyConnected(prefs.getBoolean("showAlreadyConnected", false));
        option.setAllowRemovePairRemotely(prefs.getBoolean("allowRemovePairRemotely", true));
        option.setAuthWithHMac(prefs.getBoolean("UseAuthWithHMac", false));
        option.setServerKey("key=AAAARkkdxoQ:APA91bFH_JU9abB0B7OJT-fW0rVjDac-ny13ifdjLU9VqFPp0akohPNVZvfo6mBTFBddcsbgo-pFvtYEyQ62Ohb_arw1GjEqEl4Krc7InJXTxyGqPUkz-VwgTsGzP8Gv_5ZfuqICk7S2");

        Protocol.setConnectionOption(option);
    }
}