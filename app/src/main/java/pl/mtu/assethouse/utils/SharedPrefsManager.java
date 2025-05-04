package pl.mtu.assethouse.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    private static final String PREFS_NAME = "AppSettings";
    private static final String BASE_URL_KEY = "BASE_URL";
    private static final String DEFAULT_URL = "https://rze-assethouse-t:8443/rfidentity";
    private static final String INVENTORY_DATA_KEY = "INVENTORY_DATA";
    private static final String INVENTORY_ID_KEY = "INVENTORY_ID";

    private final SharedPreferences sharedPreferences;

    public SharedPrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getBaseUrl() {
        return sharedPreferences.getString(BASE_URL_KEY, DEFAULT_URL);
    }

    public void setBaseUrl(String url) {
        sharedPreferences.edit().putString(BASE_URL_KEY, url).apply();
    }

    public void resetBaseUrl() {
        sharedPreferences.edit().putString(BASE_URL_KEY, DEFAULT_URL).apply();
    }

    public void saveInventoryData(int inventoryId, String inventoryData) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(INVENTORY_ID_KEY, inventoryId);
        editor.putString(INVENTORY_DATA_KEY, inventoryData);
        editor.apply();
    }

    public String getInventoryData() {
        return sharedPreferences.getString(INVENTORY_DATA_KEY, null);
    }

    public int getInventoryId() {
        return sharedPreferences.getInt(INVENTORY_ID_KEY, -1);
    }

    public boolean hasInventoryData() {
        return sharedPreferences.contains(INVENTORY_DATA_KEY);
    }
}
