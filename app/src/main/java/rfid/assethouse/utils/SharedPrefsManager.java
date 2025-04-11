package rfid.assethouse.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    private static final String PREFS_NAME = "AppSettings";
    private static final String BASE_URL_KEY = "BASE_URL";
    private static final String DEFAULT_URL = "https://rze-assethouse-t:8443/rfidentity";

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
}
