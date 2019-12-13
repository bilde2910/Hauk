package info.varden.hauk.system.preferences.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.system.preferences.PreferenceHandler;
import info.varden.hauk.system.preferences.ui.listener.CascadeBindListener;
import info.varden.hauk.system.preferences.ui.listener.HintBindListener;
import info.varden.hauk.system.preferences.ui.listener.InputTypeBindListener;
import info.varden.hauk.system.preferences.ui.listener.NightModeChangeListener;
import info.varden.hauk.system.preferences.ui.listener.ProxyPreferenceChangeListener;

/**
 * Settings activity that allows the user to change app preferences.
 *
 * @author Marius Lindvall
 */
public final class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static final class SettingsFragment extends PreferenceFragmentCompat {

        private Context ctx = null;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            PreferenceManager manager = getPreferenceManager();

            // Intercept all reads and writes so that values are properly validated and encrypted if
            // required by Preference.
            manager.setPreferenceDataStore(new PreferenceHandler(this.ctx));

            // Load the preferences layout.
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Set InputType and other attributes for text edit boxes.
            ((EditTextPreference) manager.findPreference(Constants.PREF_SERVER_ENCRYPTED.getKey())).setOnBindEditTextListener(new CascadeBindListener(new EditTextPreference.OnBindEditTextListener[]{
                    new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI),
                    new HintBindListener(R.string.pref_cryptServer_hint)
            }));
            ((EditTextPreference) manager.findPreference(Constants.PREF_USERNAME_ENCRYPTED.getKey())).setOnBindEditTextListener(new CascadeBindListener(new EditTextPreference.OnBindEditTextListener[]{
                    new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME),
                    new HintBindListener(R.string.pref_cryptUsername_hint)
            }));
            ((EditTextPreference) manager.findPreference(Constants.PREF_PASSWORD_ENCRYPTED.getKey())).setOnBindEditTextListener(
                    new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
            );
            ((EditTextPreference) manager.findPreference(Constants.PREF_E2E_PASSWORD.getKey())).setOnBindEditTextListener(
                    new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
            );
            ((EditTextPreference) manager.findPreference(Constants.PREF_INTERVAL.getKey())).setOnBindEditTextListener(
                    new InputTypeBindListener(InputType.TYPE_CLASS_NUMBER)
            );
            ((EditTextPreference) manager.findPreference(Constants.PREF_CUSTOM_ID.getKey())).setOnBindEditTextListener(new CascadeBindListener(new EditTextPreference.OnBindEditTextListener[]{
                    new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE),
                    new HintBindListener(R.string.pref_requestLink_hint)
            }));
            ((EditTextPreference) manager.findPreference(Constants.PREF_PROXY_HOST.getKey())).setOnBindEditTextListener(
                    new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI)
            );
            ((EditTextPreference) manager.findPreference(Constants.PREF_PROXY_PORT.getKey())).setOnBindEditTextListener(
                    new InputTypeBindListener(InputType.TYPE_CLASS_NUMBER)
            );
            ((EditTextPreference) manager.findPreference(Constants.PREF_CONNECTION_TIMEOUT.getKey())).setOnBindEditTextListener(
                    new InputTypeBindListener(InputType.TYPE_CLASS_NUMBER)
            );

            // Set proxy settings disabled if proxy is set to default or none.
            manager.findPreference(Constants.PREF_PROXY_TYPE.getKey()).setOnPreferenceChangeListener(new ProxyPreferenceChangeListener(new Preference[]{
                    manager.findPreference(Constants.PREF_PROXY_HOST.getKey()),
                    manager.findPreference(Constants.PREF_PROXY_PORT.getKey())
            }));

            // Update night mode when its preference is changed.
            manager.findPreference(Constants.PREF_NIGHT_MODE.getKey()).setOnPreferenceChangeListener(new NightModeChangeListener());
        }

        @Override
        public void onAttach(Context ctx) {
            super.onAttach(ctx);
            this.ctx = ctx;
        }
    }
}