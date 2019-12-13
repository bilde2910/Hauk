package info.varden.hauk.system.preferences.indexresolver;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * An enum preference that maps night mode styles for the app.
 *
 * @author Marius Lindvall
 */
@SuppressWarnings("unused")
public final class NightModeStyle extends Resolver<NightModeStyle, Integer> {
    private static final long serialVersionUID = 1926796368584326815L;

    public static final NightModeStyle FOLLOW_SYSTEM = new NightModeStyle(0, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    public static final NightModeStyle AUTO_BATTERY = new NightModeStyle(1, AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
    public static final NightModeStyle ALWAYS_DARK = new NightModeStyle(2, AppCompatDelegate.MODE_NIGHT_YES);
    public static final NightModeStyle NEVER_DARK = new NightModeStyle(3, AppCompatDelegate.MODE_NIGHT_NO);

    private NightModeStyle(int index, Integer mapping) {
        super(index, mapping);
    }
}
