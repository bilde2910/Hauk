package info.varden.hauk.system.powersaving;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.Nullable;

import info.varden.hauk.Constants;
import info.varden.hauk.dialog.CustomDialogBuilder;

/**
 * A dialog that prompts the user to open system settings to ignore power savings for Hauk.
 *
 * @author Marius Lindvall
 */
public final class WarningDialog implements CustomDialogBuilder {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * Preference object to save warning acknowledgement state in.
     */
    private final SharedPreferences prefs;

    /**
     * The power saving device that has been identified.
     */
    private final Device device;

    WarningDialog(Context ctx, SharedPreferences prefs, Device device) {
        this.ctx = ctx;
        this.prefs = prefs;
        this.device = device;
    }

    /**
     * Saves the fact that the user has acknowledged the warning, so they are not warned again
     * later.
     */
    private void saveAcknowledgement() {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putInt(Constants.DEVICE_PREF_WARNED_BATTERY_SAVINGS, this.device.getID());
        editor.apply();
    }

    @Override
    public void onPositive() {
        // Dismiss button clicked.
        saveAcknowledgement();
    }

    @Override
    public void onNegative() {
        // Open settings button clicked.
        saveAcknowledgement();
        this.device.launch(this.ctx);
    }

    @Nullable
    @Override
    public View createView(Context ctx) {
        return null;
    }
}
