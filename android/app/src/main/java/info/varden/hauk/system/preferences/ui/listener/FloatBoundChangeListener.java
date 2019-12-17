package info.varden.hauk.system.preferences.ui.listener;

import androidx.preference.Preference;

import info.varden.hauk.utils.Log;

/**
 * Bounds checking preference change listener that ensures the given value is between two floating
 * point values (inclusive).
 *
 * @author Marius Lindvall
 */
public final class FloatBoundChangeListener implements Preference.OnPreferenceChangeListener {
    private final float min;
    private final float max;

    public FloatBoundChangeListener(float min, float max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            float value = Float.parseFloat((String) newValue);
            return value >= this.min && value <= this.max;
        } catch (NumberFormatException ex) {
            Log.e("Number %s is not a valid float", ex, newValue); //NON-NLS
        }
        return false;
    }
}
