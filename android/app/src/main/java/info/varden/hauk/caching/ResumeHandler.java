package info.varden.hauk.caching;

import android.content.Context;

import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;

/**
 * Handler interface that should be implemented by classes capable of processing and resuming shares
 * received by the server on each location update.
 *
 * @author Marius Lindvall
 */
public interface ResumeHandler {
    /**
     * Called if there are resumable shares.
     * @param ctx     Android application context.
     * @param session The session stored in the resumption data.
     * @param shares  A list of shares stored in the resumption data. Note - these shares do not
     *                have an attached Session; it must be attached using setSession().
     */
    void onSharesFetched(Context ctx, Session session, Share[] shares);
}
