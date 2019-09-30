package info.varden.hauk;

import info.varden.hauk.struct.Version;

/**
 * Constants used in the Hauk app.
 *
 * @author Marius Lindvall
 */
public final class HaukConst {
    // Duration units.
    public static final int DURATION_UNIT_MINUTES = 0;
    public static final int DURATION_UNIT_HOURS = 1;
    public static final int DURATION_UNIT_DAYS = 2;

    // Minimum backend version supporting group shares.
    public static final Version VERSION_COMPAT_GROUP_SHARE = new Version("1.1");

    // Minimum backend version that sends the link ID as well as the view link itself.
    public static final Version VERSION_COMPAT_VIEW_ID = new Version("1.2");
}
