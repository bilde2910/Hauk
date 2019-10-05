package info.varden.hauk.ui.listener;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.EditText;

import info.varden.hauk.Constants;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.PreferenceManager;

/**
 * On-checked-change listener for the checkbox that lets users change their preference of whether or
 * not they want the app to save their password.
 *
 * @see info.varden.hauk.ui.MainActivity
 * @author Marius Lindvall
 */
public final class RememberPasswordPreferenceChangedListener implements CompoundButton.OnCheckedChangeListener {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * The text input box that contains the password.
     */
    private final EditText passwordBox;

    public RememberPasswordPreferenceChangedListener(Context ctx, EditText passwordBox) {
        this.ctx = ctx;
        this.passwordBox = passwordBox;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i("Password remember preference changed, remember=%s", isChecked); //NON-NLS
        // Update the stored password immediately. Clear the password from preferences if the box
        // was unchecked.
        PreferenceManager prefs = new PreferenceManager(this.ctx);
        prefs.set(Constants.PREF_REMEMBER_PASSWORD, isChecked);
        prefs.set(Constants.PREF_PASSWORD, isChecked ? this.passwordBox.getText().toString() : "");
    }
}
