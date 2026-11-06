package pl.mtu.assethouse.utils;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.zebra.deviceidentifierswrapper.DIHelper;
import com.zebra.deviceidentifierswrapper.IDIResultCallbacks;

import java.util.UUID;

// Stable identifier this scanner reports to the backend: the Zebra serial number printed on the
// device label, falling back to ANDROID_ID where that is unavailable. Resolved once and cached.
public class DeviceIdProvider {
    private static final String TAG = "DeviceIdProvider";
    private static final int MAX_LENGTH = 64;

    public static final String SOURCE_SERIAL = "Serial number";
    public static final String SOURCE_FALLBACK = "Fallback ID";

    private static volatile String cachedId;
    private static volatile String cachedSource;

    private DeviceIdProvider() {
    }

    // A resolved serial is never re-queried; a fallback is retried on every cold start.
    public static void resolveAsync(Context context, Runnable onResolved) {
        SharedPrefsManager prefsManager = new SharedPrefsManager(context);

        if (cachedId == null) {
            cachedId = prefsManager.getDeviceId();
            cachedSource = prefsManager.getDeviceIdSource();
        }

        if (isUsable(cachedId) && SOURCE_SERIAL.equals(cachedSource)) {
            if (onResolved != null) {
                onResolved.run();
            }
            return;
        }

        if (!isUsable(cachedId)) {
            store(prefsManager, fallbackId(context, prefsManager), SOURCE_FALLBACK);
        }

        requestSerialNumber(context, prefsManager, onResolved);
    }

    public static String get(Context context) {
        if (!isUsable(cachedId)) {
            SharedPrefsManager prefsManager = new SharedPrefsManager(context);
            cachedId = prefsManager.getDeviceId();
            cachedSource = prefsManager.getDeviceIdSource();

            if (!isUsable(cachedId)) {
                store(prefsManager, fallbackId(context, prefsManager), SOURCE_FALLBACK);
            }
        }
        return cachedId;
    }

    public static String getSource(Context context) {
        get(context);
        return cachedSource != null ? cachedSource : SOURCE_FALLBACK;
    }

    private static void requestSerialNumber(Context context, SharedPrefsManager prefsManager, Runnable onResolved) {
        try {
            DIHelper.getSerialNumber(context.getApplicationContext(), new IDIResultCallbacks() {
                @Override
                public void onSuccess(String message) {
                    if (isUsable(message)) {
                        Log.d(TAG, "Serial number retrieved");
                        store(prefsManager, message.trim(), SOURCE_SERIAL);
                    } else {
                        Log.w(TAG, "Serial number lookup returned an empty value, keeping fallback");
                    }
                    if (onResolved != null) {
                        onResolved.run();
                    }
                }

                @Override
                public void onError(String message) {
                    Log.w(TAG, "Serial number unavailable, keeping fallback: " + message);
                    if (onResolved != null) {
                        onResolved.run();
                    }
                }

                @Override
                public void onDebugStatus(String message) {
                    Log.d(TAG, message);
                }
            });
        } catch (Throwable t) {
            // Non-Zebra hardware: the com.symbol.emdk library the wrapper needs is not present.
            Log.w(TAG, "Serial number lookup unavailable on this device, keeping fallback", t);
            if (onResolved != null) {
                onResolved.run();
            }
        }
    }

    private static String fallbackId(Context context, SharedPrefsManager prefsManager) {
        String existing = prefsManager.getDeviceId();
        if (isUsable(existing)) {
            return existing;
        }

        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return isUsable(androidId) ? androidId : UUID.randomUUID().toString();
    }

    private static synchronized void store(SharedPrefsManager prefsManager, String id, String source) {
        String trimmed = id.length() > MAX_LENGTH ? id.substring(0, MAX_LENGTH) : id;
        cachedId = trimmed;
        cachedSource = source;
        prefsManager.saveDeviceId(trimmed, source);
    }

    private static boolean isUsable(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
