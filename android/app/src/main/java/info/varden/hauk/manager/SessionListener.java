package info.varden.hauk.manager;

import info.varden.hauk.struct.Session;

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
     */
    void onSessionCreated(Session session);

    /**
     * Called if the session could not be initiated due to missing location permissions.
     */
    void onSessionCreationFailedDueToPermissions();
}
