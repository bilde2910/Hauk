package info.varden.hauk.struct;

import java.io.Serializable;

/**
 * A data structure that contains parameters for a given share.
 *
 * @author Marius Lindvall
 */
public final class Share implements Serializable {
    private static final long serialVersionUID = -1922979390994061774L;

    // The session is transient to avoid it being saved as duplicate when serialized by
    // ResumableSessions. Instead, the session is attached to each share when recreated using
    // setSession().
    /**
     * The session that manages this share.
     */
    private transient Session session;

    /**
     * A public view link that others can use to follow the location of the user via the share.
     */
    private final String viewURL;

    /**
     * The ID of the share on the server. Falls back to the view URL if no ID is provided.
     */
    private final String viewID;

    /**
     * A code others can use to join a group share. Only used for group shares, null otherwise.
     */
    private final String joinCode;

    /**
     * The type of this share (alone, group, etc).
     */
    private final ShareMode type;

    /**
     * Constructs a share without a join code.
     *
     * @param session Session associated with the share.
     * @param viewURL Public view link for the share.
     * @param viewID  ID associated with the share on the server, or {@code viewURL} if unavailable.
     * @param type    The type of this share.
     */
    public Share(Session session, String viewURL, String viewID, ShareMode type) {
        this(session, viewURL, viewID, null, type);
    }

    /**
     * Constructs a share with a join code.
     *
     * @param session  Session associated with the share.
     * @param viewURL  Public view link for the share.
     * @param viewID   ID associated with the share on the server, or {@code viewURL} if
     *                 unavailable.
     * @param joinCode A code others can use to join the group share.
     * @param type     The type of this share.
     */
    public Share(Session session, String viewURL, String viewID, String joinCode, ShareMode type) {
        this.session = session;
        this.viewURL = viewURL;
        this.viewID = viewID;
        this.joinCode = joinCode;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Share{session=" + this.session
                + ",viewURL=" + this.viewURL
                + ",viewID=" + this.viewID
                + ",joinCode=" + (this.joinCode == null ? "null" : this.joinCode)
                + ",type=" + this.type
                + "}";
    }

    public Session getSession() {
        return this.session;
    }

    /**
     * Changes the session associated with this share. This function should only be used when
     * de-serializing resumable shares, where the session is undefined by default.
     *
     * @param session The session to associate the share with.
     */
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

    public ShareMode getShareMode() {
        return this.type;
    }

}
