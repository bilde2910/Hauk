package info.varden.hauk.system.preferences;

/**
 * Exception that is thrown when {@link info.varden.hauk.system.preferences.ui.SettingsActivity}
 * tries to read a setting that does not exist in Hauk.
 *
 * @author Marius Lindvall
 */
class PreferenceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 6201186189243885309L;

    PreferenceNotFoundException(String key) {
        super(String.format("Preference %s was requested but does not exist in Hauk", key)); //NON-NLS
    }
}
