package info.varden.hauk.struct;

import java.io.Serializable;

import info.varden.hauk.HaukConst;

/**
 * A data structure that contains parameters for a given share.
 *
 * @author Marius Lindvall
 */
public class Share implements Serializable {
    // The session is transient to avoid it being saved as duplicate when serialized by
    // ResumableSessions. Instead, the session is attached to each share when recreated using
    // setSession().
    private transient Session session;

    private final String viewURL;
    private final String viewID;
    private final String joinCode;
    private final int type;

    public Share(Session session, String viewURL, String viewID, int type) {
        this(session, viewURL, viewID, null, type);
    }

    public Share(Session session, String viewURL, String viewID, String joinCode, int type) {
        this.session = session;
        this.viewURL = viewURL;
        this.viewID = viewID;
        this.joinCode = joinCode;
        this.type = type;
    }

    public Session getSession() {
        return this.session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getViewURL() {
        return this.viewURL;
    }

    public String getID() {
        return this.viewID;
    }

    public String getJoinCode() {
        return this.joinCode;
    }

    public int getShareMode() {
        return this.type;
    }

    public int getLinkType() {
        switch (getShareMode()) {
            case HaukConst.SHARE_MODE_CREATE_ALONE:
                return HaukConst.LINK_TYPE_ALONE;
            case HaukConst.SHARE_MODE_CREATE_GROUP:
                return HaukConst.LINK_TYPE_GROUP_HOST;
            case HaukConst.SHARE_MODE_JOIN_GROUP:
                return HaukConst.LINK_TYPE_GROUP_MEMBER;
            default:
                return 0;
        }
    }
}
