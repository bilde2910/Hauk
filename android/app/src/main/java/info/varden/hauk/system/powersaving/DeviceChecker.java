package info.varden.hauk.system.powersaving;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.dialog.Buttons;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.utils.Log;

/**
 * A checker class that checks if the device Hauk is currently running on has system battery saving
 * settings that are so aggressive that they can interfere with Hauk's operation.
 *
 * @author Marius Lindvall
 */
public final class DeviceChecker {
    /**
     * Android application context.
     */
    private final Context ctx;

    public DeviceChecker(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Checks whether or not the current device has aggressive battery savings, and shows a warning
     * dialog if this is the case.
     */
    public void performCheck() {
        Log.i("Checking for aggressive battery savings"); //NON-NLS
        Device device = identifyDevice();
        if (device != null) {
            Log.i("Found device %s, checking for user prompt state", device); //NON-NLS

            // The user should only be displayed the warning once, so we check if the user has already
            // acknowledged the warning in device spec settings.
            SharedPreferences prefs = this.ctx.getSharedPreferences(Constants.SHARED_PREFS_DEVICE_SPECS, Context.MODE_PRIVATE);
            if (prefs.getInt(Constants.DEVICE_PREF_WARNED_BATTERY_SAVINGS, -1) != device.getID()) {

                // If not, show a warning to the user about their device's battery saving settings.
                Log.i("User has not been warned about this device previously, prompting"); //NON-NLS
                DialogService dialogSvc = new DialogService(this.ctx);
                dialogSvc.showDialog(
                        R.string.battery_savings_title,
                        String.format(this.ctx.getString(R.string.battery_savings_body), this.ctx.getString(device.getManufacturerStringResource())),
                        Buttons.Two.SETTINGS_DISMISS,
                        new WarningDialog(this.ctx, prefs, device)
                );
            } else {
                Log.i("User has been warned about this device previously, ignoring"); //NON-NLS
            }
        } else {
            Log.i("Device does not have aggressive battery saving functions"); //NON-NLS
        }
    }

    /**
     * Tries to identify a device from the list of devices in {@link Device}.
     *
     * @return A device if one matches, null otherwise.
     */
    @Nullable
    private static Device identifyDevice() {
        for (Device device : Device.values()) {
            Log.d("Checking device %s", device); //NON-NLS
            if (device.matches()) {
                return device;
            }
        }
        return null;
    }
}
