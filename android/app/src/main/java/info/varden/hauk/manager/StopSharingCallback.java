package info.varden.hauk.manager;

import info.varden.hauk.http.FailureHandler;

/**
 * Callback interface that is called upon when the user requests to stop the share.
 *
 * @author Marius Lindvall
 */
public interface StopSharingCallback extends FailureHandler {
    /**
     * Called if the share was successfully stopped.
     */
    void onSuccess();

    /**
     * Called if the share could not be stopped because the share or session did not exist.
     */
    void onShareNull();
}
