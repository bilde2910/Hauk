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

    // Share creation modes.
    public static final int SHARE_MODE_CREATE_ALONE = 0;
    public static final int SHARE_MODE_CREATE_GROUP = 1;
    public static final int SHARE_MODE_JOIN_GROUP = 2;

    // Link types displayed in the list of active links.
    public static final int LINK_TYPE_ALONE = R.string.link_type_solo;
    public static final int LINK_TYPE_GROUP_HOST = R.string.link_type_group_host;
    public static final int LINK_TYPE_GROUP_MEMBER = R.string.link_type_group_member;

    // Minimum backend version supporting group shares.
    public static final Version VERSION_COMPAT_GROUP_SHARE = new Version("1.1");

    // Minimum backend version that sends the link ID as well as the view link itself.
    public static final Version VERSION_COMPAT_VIEW_ID = new Version("1.2");
}
