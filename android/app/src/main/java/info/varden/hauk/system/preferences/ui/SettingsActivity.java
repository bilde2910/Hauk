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

import info.varden.hauk.BuildConfig;
import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.system.preferences.PreferenceHandler;
import info.varden.hauk.system.preferences.ui.listener.CascadeBindListener;
import info.varden.hauk.system.preferences.ui.listener.CascadeChangeListener;
import info.varden.hauk.system.preferences.ui.listener.FloatBoundChangeListener;
import info.varden.hauk.system.preferences.ui.listener.HintBindListener;
import info.varden.hauk.system.preferences.ui.listener.InputTypeBindListener;
import info.varden.hauk.system.preferences.ui.listener.IntegerBoundChangeListener;
import info.varden.hauk.system.preferences.ui.listener.NightModeChangeListener;
import info.varden.hauk.system.preferences.ui.listener.ProxyPreferenceChangeListener;
import info.varden.hauk.ui.listener.OpenLinkListener;
import info.varden.hauk.utils.Log;

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
            setTextEditParams(manager, Constants.PREF_SERVER_ENCRYPTED, new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI), new HintBindListener(R.string.pref_cryptServer_hint));
            setTextEditParams(manager, Constants.PREF_USERNAME_ENCRYPTED, new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME), new HintBindListener(R.string.pref_cryptUsername_hint));
            setTextEditParams(manager, Constants.PREF_PASSWORD_ENCRYPTED, new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            setTextEditParams(manager, Constants.PREF_E2E_PASSWORD, new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            setTextEditParams(manager, Constants.PREF_INTERVAL, new InputTypeBindListener(InputType.TYPE_CLASS_NUMBER));
            setTextEditParams(manager, Constants.PREF_UPDATE_DISTANCE, new InputTypeBindListener(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
            setTextEditParams(manager, Constants.PREF_CUSTOM_ID, new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE), new HintBindListener(R.string.pref_requestLink_hint));
            setTextEditParams(manager, Constants.PREF_PROXY_HOST, new InputTypeBindListener(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI));
            setTextEditParams(manager, Constants.PREF_PROXY_PORT, new InputTypeBindListener(InputType.TYPE_CLASS_NUMBER));
            setTextEditParams(manager, Constants.PREF_CONNECTION_TIMEOUT, new InputTypeBindListener(InputType.TYPE_CLASS_NUMBER));

            // Set value bounds checks.
            setChangeListeners(manager, Constants.PREF_INTERVAL, new IntegerBoundChangeListener(1, Integer.MAX_VALUE));
            setChangeListeners(manager, Constants.PREF_UPDATE_DISTANCE, new FloatBoundChangeListener(0.0F, Float.MAX_VALUE));
            setChangeListeners(manager, Constants.PREF_PROXY_PORT, new IntegerBoundChangeListener(Constants.PORT_MIN, Constants.PORT_MAX));
            setChangeListeners(manager, Constants.PREF_CONNECTION_TIMEOUT, new IntegerBoundChangeListener(1, Integer.MAX_VALUE));

            // Set proxy settings disabled if proxy is set to default or none.
            setChangeListeners(manager, Constants.PREF_PROXY_TYPE, new ProxyPreferenceChangeListener(new Preference[]{
                    manager.findPreference(Constants.PREF_PROXY_HOST.getKey()),
                    manager.findPreference(Constants.PREF_PROXY_PORT.getKey())
            }));

            // Update night mode when its preference is changed.
            setChangeListeners(manager, Constants.PREF_NIGHT_MODE, new NightModeChangeListener());

            manager.findPreference("dummy_version").setSummary(BuildConfig.VERSION_NAME);
            manager.findPreference("dummy_sourceCode").setOnPreferenceClickListener(new OpenLinkListener(this.ctx, R.string.label_source_link));
            manager.findPreference("dummy_reportIssue").setOnPreferenceClickListener(new OpenLinkListener(this.ctx, R.string.link_issue_tracker));
        }

        private static void setTextEditParams(PreferenceManager manager, info.varden.hauk.system.preferences.Preference<?> preference, EditTextPreference.OnBindEditTextListener... listeners) {
            EditTextPreference pref = manager.findPreference(preference.getKey());
            if (pref != null) {
                pref.setOnBindEditTextListener(new CascadeBindListener(listeners));
            } else {
                Log.wtf("Could not find setting for preference %s setting OnBindEditTextListener", preference); //NON-NLS
            }
        }

        private static void setChangeListeners(PreferenceManager manager, info.varden.hauk.system.preferences.Preference<?> preference, Preference.OnPreferenceChangeListener... listeners) {
            Preference pref = manager.findPreference(preference.getKey());
            if (pref != null) {
                pref.setOnPreferenceChangeListener(new CascadeChangeListener(listeners));
            } else {
                Log.wtf("Could not find setting for preference %s when setting OnPreferenceChangeListener", preference); //NON-NLS
            }
        }

        @Override
        public void onAttach(Context ctx) {
            super.onAttach(ctx);
            this.ctx = ctx;
        }
    }
}