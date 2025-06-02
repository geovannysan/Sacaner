package com.example.myapplication.Infraestructura;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class DeviceIdManager {
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private static String uniqueID = null;

    public synchronized static String getUniqueID(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                sharedPrefs.edit().putString(PREF_UNIQUE_ID, uniqueID).apply();
                return uniqueID;
            }
            return uniqueID;
        }
        return uniqueID;
    }
}
