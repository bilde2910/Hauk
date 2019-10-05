package info.varden.hauk.manager;

import android.content.Context;

import info.varden.hauk.caching.ResumableSessions;
import info.varden.hauk.caching.ResumeHandler;
import info.varden.hauk.caching.ResumePrompt;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.utils.Log;

/**
 * {@link ResumeHandler} implementation used by {@link SessionManager} to prompt users for whether
 * or not they want to resume stored sessions, as well as handling their response to said prompt.
 *
 * @author Marius Lindvall
 */
public final class AutoResumptionPrompter implements ResumeHandler {
    /**
     * The session manager to call to resume the shares if the user indicates that they want to
     * resume the shares.
     */
    private final SessionManager manager;

    /**
     * The manager's resumption handler. This is used to clear the resumption data before the shares
     * are resumed by the session manager, as the session manager will re-flag the shares as
     * resumable when it adds them to its internal share list.
     */
    private final ResumableSessions resumptionHandler;

    /**
     * The implementation that prompts the user for whether or not they want to resume the shares.
     */
    private final ResumePrompt prompt;

    AutoResumptionPrompter(SessionManager manager, ResumableSessions resumptionHandler, ResumePrompt prompt) {
        this.manager = manager;
        this.resumptionHandler = resumptionHandler;
        this.prompt = prompt;
    }

    @Override
    public void onSharesFetched(Context ctx, Session session, Share[] shares) {
        Log.i("%s resumable share(s) found for session %s", shares.length, session); //NON-NLS
        // The shares provided by ResumableSessions do not have a session attached to them. Attach
        // it to the shares so that they can be shown properly by the prompt and so that the updates
        // have a backend to be broadcast to if the shares are resumed.
        for (Share share : shares) {
            share.setSession(session);
        }
        this.prompt.promptForResumption(ctx, session, shares, new Callback(shares));
    }

    /**
     * A prompt callback provided to the {@link ResumePrompt} to handle user response.
     */
    private final class Callback implements PromptCallback {
        /**
         * The list of shares to resume.
         */
        private final Share[] shares;

        private Callback(Share[] shares) {
            this.shares = shares;
        }

        @Override
        public void accept() {
            // If yes, do continue the session.
            Log.i("Resuming shares..."); //NON-NLS
            AutoResumptionPrompter.this.resumptionHandler.clearResumableSession();
            for (Share share : this.shares) {
                AutoResumptionPrompter.this.manager.shareLocation(share);
            }
        }

        @Override
        public void deny() {
            // If not, clear the resumption data so that the user isn't asked again for
            // the share in question.
            Log.i("Shares are not resumed"); //NON-NLS
            AutoResumptionPrompter.this.resumptionHandler.clearResumableSession();
        }
    }
}
