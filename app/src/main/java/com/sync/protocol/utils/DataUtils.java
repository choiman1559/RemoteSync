package com.sync.protocol.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

public class DataUtils {
    @SuppressLint("HardwareIds")
    public static String getUniqueID(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String str = "";
        if (prefs != null) {
            switch (prefs.getString("uniqueIdMethod", "Globally-Unique ID")) {
                case "Globally-Unique ID":
                    str = prefs.getString("GUIDPrefix", "");
                    break;

                case "Android ID":
                    str = prefs.getString("AndroidIDPrefix", "");
                    break;

                case "Firebase IID":
                    str = prefs.getString("FirebaseIIDPrefix", "");
                    break;

                case "Device MAC ID":
                    str = prefs.getString("MacIDPrefix", "");
                    break;
            }
            return str;
        }
        return "";
    }
}
