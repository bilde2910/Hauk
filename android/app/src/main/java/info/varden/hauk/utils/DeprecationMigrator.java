package info.varden.hauk.utils;

import android.content.Context;

import info.varden.hauk.Constants;

/**
 * Helper utility to migrate old, deprecated settings saved in shared preferences to modern storage.
 *
 * @author Marius Lindvall
 */
public final class DeprecationMigrator {
    /**
     * Android application context.
     */
    private final Context ctx;

    public DeprecationMigrator(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Checks if there are un-migrated settings, and migrates these.
     */
    public void migrate() {
        PreferenceManager prefs = new PreferenceManager(this.ctx);
        if (prefs.has(Constants.PREF_SERVER)) {
            Log.i("Encrypting previously stored server"); //NON-NLS
            String server = prefs.get(Constants.PREF_SERVER);
            prefs.set(Constants.PREF_SERVER_ENCRYPTED, server);
            prefs.clear(Constants.PREF_SERVER);
        }
        if (prefs.has(Constants.PREF_USERNAME)) {
            Log.i("Encrypting previously stored username"); //NON-NLS
            String user = prefs.get(Constants.PREF_USERNAME);
            prefs.set(Constants.PREF_USERNAME_ENCRYPTED, user);
            prefs.clear(Constants.PREF_USERNAME);
        }
        if (prefs.has(Constants.PREF_PASSWORD)) {
            Log.i("Encrypting previously stored password"); //NON-NLS
            String pass = prefs.get(Constants.PREF_PASSWORD);
            prefs.set(Constants.PREF_PASSWORD_ENCRYPTED, pass);
            prefs.clear(Constants.PREF_PASSWORD);
        }
        if (!prefs.has(Constants.PREF_ENABLE_E2E)) {
            boolean enableE2E = !prefs.get(Constants.PREF_E2E_PASSWORD).isEmpty();
            Log.i("Setting E2E enabled preference to %s based on stored preferences", String.valueOf(enableE2E)); //NON-NLS
            prefs.set(Constants.PREF_ENABLE_E2E, enableE2E);
        }
    }
}
