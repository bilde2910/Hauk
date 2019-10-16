package info.varden.hauk.system.powersaving;

import android.content.Context;
import android.os.Build;

import java.util.regex.Pattern;

import info.varden.hauk.R;
import info.varden.hauk.system.launcher.ActionLauncher;
import info.varden.hauk.system.launcher.ComponentLauncher;
import info.varden.hauk.system.launcher.Launcher;

/**
 * A list of power saving devices. These devices enforce aggressive power saving settings that
 * interfere with the operation of foreground services, causing Hauk to stop working. This list is
 * used to notify the user that this can happen, and prompts them to open settings to ensure Hauk is
 * whitelisted.
 *
 * @author Marius Lindvall
 */
public enum Device {
    @SuppressWarnings({"HardCodedStringLiteral", "SpellCheckingInspection"})
    HUAWEI(1, R.string.manufacturer_huawei, Pattern.compile("^FRD-[A-Z0-9]+$"), new ComponentLauncher("com.huawei.systemmanager", ".optimize.process.ProtectActivity")),
    @SuppressWarnings({"HardCodedStringLiteral"})
    ONEPLUS(2, R.string.manufacturer_oneplus, Pattern.compile("^ONEPLUS "), new ActionLauncher("com.android.settings.action.BACKGROUND_OPTIMIZE"));

    /**
     * A unique internal identifier for this device.
     */
    private final int id;

    /**
     * A string resource representing the name of the device manufacturer.
     */
    private final int manufacturer;

    /**
     * A regular expression matched against the build number of the device, used to identify the
     * device ROM.
     */
    private final Pattern buildRegex;

    /**
     * A launcher that opens system battery saving settings.
     */
    private final Launcher launcher;

    Device(int id, int manufacturer, Pattern buildRegex, Launcher launcher) {
        this.id = id;
        this.manufacturer = manufacturer;
        this.buildRegex = buildRegex;
        this.launcher = launcher;
    }

    /**
     * Checks whether or not the given device matches the device the app is currently running on.
     */
    public boolean matches() {
        return this.buildRegex.matcher(Build.DISPLAY).find();
    }

    /**
     * Opens system battery saving settings.
     *
     * @param ctx Android application context.
     */
    public void launch(Context ctx) {
        this.launcher.launch(ctx);
    }

    public int getID() {
        return this.id;
    }

    public int getManufacturerStringResource() {
        return this.manufacturer;
    }

    @Override
    public String toString() {
        return "Device{"
                + "id=" + this.id
                + ",manufacturer=" + this.manufacturer
                + ",pattern=" + this.buildRegex
                + ",launchSpec=" + this.launcher
                + "}";
    }
}
