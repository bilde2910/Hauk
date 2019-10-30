package info.varden.hauk.manager;

import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;

/**
 * Callback interface that {@link SessionManager} handlers can attach to receive status updates
 * about session creation.
 *
 * @author Marius Lindvall
 */
public interface SessionListener {
    /**
     * Called whenever a new session is created.
     *
     * @param session The session that was created.
     * @param share   The share that the session was created for.
     */
    void onSessionCreated(Session session, Share share);

    /**
     * Called if the session could not be initiated due to missing location permissions.
     */
    void onSessionCreationFailedDueToPermissions();
}
