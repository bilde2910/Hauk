package info.varden.hauk.manager;

import info.varden.hauk.http.FailureHandler;
import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.struct.Version;

/**
 * A response handler interface that must be provided when starting a new session in order to
 * receive callbacks when the session initiation packet has been responded to.
 *
 * @author Marius Lindvall
 */
public interface SessionInitiationResponseHandler extends FailureHandler {
    /**
     * Called when the session initiation packet is being sent.
     */
    void onInitiating();

    /**
     * Called if the session was successfully initiated.
     */
    void onSuccess();

    /**
     * Called if the session was successfully initiated, but the backend is not compatible with the
     * share mode that was requested by the user. {@code onSuccess()} will still be called after
     * this callback in the event that this happens.
     *
     * @param downgradeTo    The sharing mode that was picked instead of the requested mode.
     * @param backendVersion The version of the Hauk backend server.
     */
    void onShareModeForciblyDowngraded(ShareMode downgradeTo, Version backendVersion);
}
