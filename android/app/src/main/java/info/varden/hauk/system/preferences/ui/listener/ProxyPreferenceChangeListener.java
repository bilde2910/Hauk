package info.varden.hauk.system.preferences.ui.listener;

import androidx.preference.Preference;

import info.varden.hauk.system.preferences.indexresolver.ProxyTypeResolver;

/**
 * Value change listener for the proxy type selection preference that disables the other proxy
 * settings if a selection is made to the type that makes the other proxy settings unnecessary.
 *
 * @author Marius Lindvall
 */
public final class ProxyPreferenceChangeListener implements Preference.OnPreferenceChangeListener {
    private final Preference[] prefsToDisable;

    public ProxyPreferenceChangeListener(Preference[] prefsToDisable) {
        this.prefsToDisable = prefsToDisable.clone();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int choice = Integer.valueOf((String) newValue);
        boolean enable = choice != ProxyTypeResolver.SYSTEM_DEFAULT.getIndex() && choice != ProxyTypeResolver.DIRECT.getIndex();
        for (Preference pref : this.prefsToDisable) {
            pref.setEnabled(enable);
        }
        return true;
    }
}
