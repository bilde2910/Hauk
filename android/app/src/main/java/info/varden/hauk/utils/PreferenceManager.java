package info.varden.hauk.utils;

import android.content.Context;
import android.content.SharedPreferences;

import info.varden.hauk.Constants;

/**
 * Utility class that manages connection preferences in Hauk.
 *
 * @author Marius Lindvall
 */
public final class PreferenceManager {
    private final SharedPreferences prefs;

    public PreferenceManager(Context ctx) {
        this.prefs = ctx.getSharedPreferences(Constants.SHARED_PREFS_CONNECTION, Context.MODE_PRIVATE);
    }

    /**
     * Returns the value of a preference from the saved app preferences.
     *
     * @param pair The preference to return the current value for.
     * @param <T>  The type of preference to return.
     * @return The value of the preference, or its default if not defined.
     * @see Constants
     */
    public <T> T get(Preference<T> pair) {
        Log.v("Getting preference %s", pair); //NON-NLS
        return pair.get(this.prefs);
    }

    /**
     * Sets the value of a preference and saves it to device storage.
     *
     * @param pair  The preference whose value to update.
     * @param value The value to save for the preference.
     * @param <T>   The type of preference to set.
     * @see Constants
     */
    public <T> void set(Preference<T> pair, T value) {
        Log.v("Setting preference %s, value=%s", pair, pair.isSensitive() ? "<hidden>" : value); //NON-NLS
        SharedPreferences.Editor editor = this.prefs.edit();
        pair.set(editor, value);
        editor.apply();
    }

    /**
     * Checks whether or not a preference exists in device storage.
     *
     * @param pair The preference to check for existence of.
     * @param <T>  The type of preference to check.
     * @return true if the preference exists, false otherwise.
     */
    public <T> boolean has(Preference<T> pair) {
        return pair.has(this.prefs);
    }

    /**
     * Clears the given preference from device storage.
     *
     * @param pair The preference to clear.
     * @param <T>  The type of preference to clear.
     */
    public <T> void clear(Preference<T> pair) {
        Log.v("Clearing preference %s", pair); //NON-NLS
        SharedPreferences.Editor editor = this.prefs.edit();
        pair.clear(editor);
        editor.apply();
    }
}
