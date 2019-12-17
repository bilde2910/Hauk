package info.varden.hauk.system.preferences.ui.listener;

import androidx.preference.Preference;

/**
 * Preference change listener that cascades the change event to several
 * {@link androidx.preference.Preference.OnPreferenceChangeListener}s.
 *
 * @author Marius Lindvall
 */
public final class CascadeChangeListener implements Preference.OnPreferenceChangeListener {
    private final Preference.OnPreferenceChangeListener[] listeners;

    public CascadeChangeListener(Preference.OnPreferenceChangeListener[] listeners) {
        this.listeners = listeners.clone();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        for (Preference.OnPreferenceChangeListener listener : this.listeners) {
            if (!listener.onPreferenceChange(preference, newValue)) return false;
        }
        return true;
    }
}
