package info.varden.hauk.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import info.varden.hauk.Constants;
import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.system.preferences.PreferenceManager;
import info.varden.hauk.utils.Log;

/**
 * Prompt that confirms with the user if they really intended to stop sharing their location.
 *
 * @author Marius Lindvall
 */
public final class StopSharingConfirmationPrompt implements CustomDialogBuilder.Three {
    private final PreferenceManager prefs;
    private final SessionManager manager;

    public StopSharingConfirmationPrompt(PreferenceManager prefs, SessionManager manager) {
        this.prefs = prefs;
        this.manager = manager;
    }

    @Override
    public void onNeutral() {
        Log.i("Disabling future confirmation prompts when stopping shares"); //NON-NLS
        this.prefs.set(Constants.PREF_CONFIRM_STOP, false);
        this.manager.stopSharing();
    }

    @Override
    public void onPositive() {
        this.manager.stopSharing();
    }

    @Override
    public void onNegative() {
        // Do nothing.
    }

    @Nullable
    @Override
    public View createView(Context ctx) {
        return null;
    }
}