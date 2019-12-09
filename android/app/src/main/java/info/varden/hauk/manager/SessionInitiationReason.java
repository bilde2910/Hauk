package info.varden.hauk.manager;

import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;

/**
 * Describes the reason a session was initiated.
 *
 * @author Marius Lindvall
 */
public enum SessionInitiationReason {
    /**
     * The user requested to start a new sharing session.
     */
    USER_STARTED,

    /**
     * The user requested to resume a previous sharing session.
     */
    USER_RESUMED,

    /**
     * The sharing session is automatically resumed as a result of a relaunch of the location
     * sharing service.
     */
    SERVICE_RELAUNCH,

    /**
     * The session was created because a share was added to it. This should never be received by
     * {@link SessionListener#onSessionCreated(Session, Share, SessionInitiationReason)} under any
     * normal circumstances.
     */
    SHARE_ADDED
}
