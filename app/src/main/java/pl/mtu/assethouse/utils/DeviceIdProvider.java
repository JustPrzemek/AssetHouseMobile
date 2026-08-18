package pl.mtu.assethouse.utils;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.zebra.deviceidentifierswrapper.DIHelper;
import com.zebra.deviceidentifierswrapper.IDIResultCallbacks;

import java.util.UUID;

/**
 * Single source of the stable identifier this scanner reports to the backend.
 * <p>
 * Preferred value is the Zebra serial number printed on the device label, so an administrator
 * can register the scanner in the web app by reading the hardware. If that is unavailable
 * (non-Zebra hardware, EMDK service missing, MX registration refused) we fall back to
 * {@link Settings.Secure#ANDROID_ID} so the app keeps working.
 * <p>
 * Resolved once and cached — call {@link #resolveAsync} at startup, then {@link #get} anywhere.
 * Swap the serial lookup here and no call site needs to change.
 */
public class DeviceIdProvider {
    private static final String TAG = "DeviceIdProvider";
    private static final int MAX_LENGTH = 64;

    public static final String SOURCE_SERIAL = "Serial number";
    public static final String SOURCE_FALLBACK = "Fallback ID";

    private static volatile String cachedId;
    private static volatile String cachedSource;

    private DeviceIdProvider() {
    }

    /**
     * Resolves the identifier if it is not already known. Returns immediately; the serial lookup
     * is asynchronous. A usable fallback is always in place before this returns, so callers of
     * {@link #get} never block and never see a blank value.
     * <p>
     * A previously resolved serial is never looked up again. A previously resolved fallback IS
     * retried, so a transient EMDK failure does not pin this device to the wrong identifier.
     *
     * @param onResolved optional, run when the serial lookup settles. May run on any thread.
     */
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

    /**
     * The identifier to send to the backend. Never null, never blank.
     */
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

    /**
     * Human readable description of where {@link #get} came from, for the settings screen.
     */
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

    /**
     * ANDROID_ID survives reinstalls and only resets on factory reset. On the rare device where it
     * is unavailable we persist a random UUID instead, which is stable for as long as the app data is.
     */
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
