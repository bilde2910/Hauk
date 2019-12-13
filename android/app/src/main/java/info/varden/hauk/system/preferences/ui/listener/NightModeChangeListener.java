package info.varden.hauk.system.preferences.ui.listener;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;

import info.varden.hauk.system.preferences.IndexedEnum;
import info.varden.hauk.system.preferences.indexresolver.NightModeStyle;
import info.varden.hauk.utils.Log;

/**
 * Value change listener for the night mode preference that sets the new night mode style on
 * selection.
 *
 * @author Marius Lindvall
 */
public final class NightModeChangeListener implements Preference.OnPreferenceChangeListener {

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            // Resolve the night mode (an instance of NightModeStyle is required for this).
            int mode = IndexedEnum.fromIndex(NightModeStyle.class, Integer.valueOf((String) newValue)).resolve();
            Log.i("Setting night mode %s", mode); //NON-NLS
            AppCompatDelegate.setDefaultNightMode(mode);
            return true;
        } catch (Exception e) {
            Log.e("Could not determine night mode style for value %s", e, newValue); //NON-NLS
            return false;
        }
    }
}
