package info.varden.hauk.ui.listener;

import android.content.Context;
import android.view.View;
import android.widget.CompoundButton;

import info.varden.hauk.Constants;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.PreferenceManager;

/**
 * On-checked-change listener for the checkbox that lets users change their preference of whether or
 * not they want to enable end-to-end encryption.
 *
 * @see info.varden.hauk.ui.MainActivity
 * @author Marius Lindvall
 */
public final class EncryptionEnabledChangeListener implements CompoundButton.OnCheckedChangeListener {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * The list of views which should be enabled/disabled when the checkbox is changed.
     */
    private final View[] e2eViews;

    public EncryptionEnabledChangeListener(Context ctx, View[] views) {
        this.ctx = ctx;
        this.e2eViews = views.clone();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Log.i("End-to-end encryption preference changed, enabled=%s", isChecked); //NON-NLS
        // Show/hide the end-to-end encryption views.
        PreferenceManager prefs = new PreferenceManager(this.ctx);
        for (View view : this.e2eViews) {
            view.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        }
        prefs.set(Constants.PREF_ENABLE_E2E, isChecked);
    }
}
