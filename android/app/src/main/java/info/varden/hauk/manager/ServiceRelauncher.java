package info.varden.hauk.manager;

import android.content.Context;

import info.varden.hauk.caching.ResumableSessions;
import info.varden.hauk.caching.ResumeHandler;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.utils.Log;

/**
 * {@link ResumeHandler} implementation used by {@link SessionManager} to automatically resume
 * sessions following a service relaunch. This can happen if the main activity is terminated, but
 * the share itself keeps running in the background.
 *
 * @author Marius Lindvall
 */
public final class ServiceRelauncher implements ResumeHandler {
    /**
     * The session manager to call to resume the shares.
     */
    private final SessionManager manager;

    /**
     * The manager's resumption handler. This is used to clear the resumption data before the shares
     * are resumed by the session manager, as the session manager will re-flag the shares as
     * resumable when it adds them to its internal share list.
     */
    private final ResumableSessions resumptionHandler;

    ServiceRelauncher(SessionManager manager, ResumableSessions resumptionHandler) {
        this.manager = manager;
        this.resumptionHandler = resumptionHandler;
    }

    @Override
    public void onSharesFetched(Context ctx, Session session, Share[] shares) {
        Log.i("Resuming %s share(s) automatically found for session %s", shares.length, session); //NON-NLS
        // The shares provided by ResumableSessions do not have a session attached to them. Attach
        // it to the shares so that they can be shown properly by the prompt and so that the updates
        // have a backend to be broadcast to when the shares are resumed.
        this.resumptionHandler.clearResumableSession();
        for (Share share : shares) {
            share.setSession(session);
            this.manager.shareLocation(share, SessionInitiationReason.SERVICE_RELAUNCH);
        }
    }
}
