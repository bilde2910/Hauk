package info.varden.hauk.system.preferences.ui.listener;

import androidx.preference.Preference;

import info.varden.hauk.utils.Log;

/**
 * Bounds checking preference change listener that ensures the given value is between two integer
 * values (inclusive).
 *
 * @author Marius Lindvall
 */
public final class IntegerBoundChangeListener implements Preference.OnPreferenceChangeListener {
    private final int min;
    private final int max;

    public IntegerBoundChangeListener(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            int value = Integer.parseInt((String) newValue);
            return value >= this.min && value <= this.max;
        } catch (NumberFormatException ex) {
            Log.e("Number %s is not a valid integer", ex, newValue); //NON-NLS
        }
        return false;
    }
}
