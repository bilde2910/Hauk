package info.varden.hauk.caching;

import android.content.Context;

import info.varden.hauk.manager.PromptCallback;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;

/**
 * Interface to be implemented by classes that can provide a prompt asking the user if they want to
 * resume a given session with an associated list of shares.
 *
 * @author Marius Lindvall
 */
public interface ResumePrompt {
    /**
     * Called to prompt the user to resume a given list of shares. The user response should be
     * returned by calling accept() or deny() accordingly on the provided prompt callback.
     *
     * @param ctx      Android application context.
     * @param session  The session that can be resumed.
     * @param shares   A list of resumable shares.
     * @param response The callback that should be used to indicate the user's decision.
     */
    void promptForResumption(Context ctx, Session session, Share[] shares, PromptCallback response);
}
