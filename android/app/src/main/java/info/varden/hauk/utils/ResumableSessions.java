package info.varden.hauk.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import info.varden.hauk.BuildConfig;
import info.varden.hauk.HaukConst;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;

/**
 * If the Hauk app crashes or shuts down, the app should give the option to resume any interrupted
 * shares. This class handles this functionality.
 */
public class ResumableSessions {
    private final Context ctx;
    private final SharedPreferences prefs;

    public ResumableSessions(Context ctx) {
        this.ctx = ctx;
        this.prefs = ctx.getSharedPreferences(HaukConst.SHARED_PREFS_RESUMABLE, Context.MODE_PRIVATE);
    }

    /**
     * If the app crashed, or phone restarted, Hauk gives the option to resume interrupted shares.
     * This function checks if any incomplete shares are saved on the phone and asks the user if
     * they want to resume them.
     */
    public void tryResumeShare(ResumeHandler handler) {
        if (this.prefs.getBoolean(HaukConst.RESUME_AVAILABLE, false)) {

            // Check if the app version that wrote the resumption data is the same as this session
            // to avoid deserialization errors due to incompatibilities.
            String writeVersion = this.prefs.getString(HaukConst.RESUME_CLIENT_VERSION, "");
            if (writeVersion.equals(BuildConfig.VERSION_NAME)) {

                // Get session parameters.
                final Session session = new StringSerializer<Session>().deserialize(this.prefs.getString(HaukConst.RESUME_SESSION_PARAMS, null));
                final List<Share> shares = new StringSerializer<ArrayList<Share>>().deserialize(this.prefs.getString(HaukConst.RESUME_SHARE_PARAMS, null));

                // Check that the session is still valid.
                if (session != null && !session.hasExpired() && shares != null && shares.size() > 0) {
                    handler.onSharesFetched(this.ctx, session, shares);
                } else {
                    clearResumableSession();
                }
            }
        }
    }

    /**
     * Saves session resumption data. This allows shares to be continued if the app crashes or is
     * otherwise closed.
     *
     * @param session The session to save resumption data for.
     */
    public void setSessionResumable(Session session) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(HaukConst.RESUME_AVAILABLE, true);
        editor.putString(HaukConst.RESUME_CLIENT_VERSION, BuildConfig.VERSION_NAME);
        editor.putString(HaukConst.RESUME_SESSION_PARAMS, new StringSerializer<Session>().serialize(session));
        editor.apply();
    }

    /**
     * Saves share resumption data. This allows the share to be continued if the app crashes or is
     * otherwise closed.
     *
     * @param share The share to save resumption data for.
     */
    public void addShareResumable(Share share) {
        // Get the current list of resumable shares.
        StringSerializer<ArrayList<Share>> serializer = new StringSerializer<>();
        ArrayList<Share> shares = serializer.deserialize(this.prefs.getString(HaukConst.RESUME_SHARE_PARAMS, null));
        if (shares == null) shares = new ArrayList<>();

        // Add the share and save the updated list.
        shares.add(share);
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(HaukConst.RESUME_SHARE_PARAMS, serializer.serialize(shares));
        editor.apply();
    }

    /**
     * Removes a share from the list of resumable shares.
     *
     * @param shareID The ID of the share to remove.
     */
    public void removeResumableShare(String shareID) {
        // Get the current list of resumable shares.
        StringSerializer<ArrayList<Share>> serializer = new StringSerializer<>();
        ArrayList<Share> shares = serializer.deserialize(this.prefs.getString(HaukConst.RESUME_SHARE_PARAMS, null));
        if (shares == null) return;

        // Remove the share and save the updated list.
        for (Iterator<Share> iter = shares.iterator(); iter.hasNext();) {
            Share s = iter.next();
            if (s.getID().equals(shareID)) {
                iter.remove();
            }
        }
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(HaukConst.RESUME_SHARE_PARAMS, serializer.serialize(shares));
        editor.apply();
    }

    /**
     * Clears saved resumable session data.
     */
    public void clearResumableSession() {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * A handler that is called with a list of shares that can be resumed.
     */
    public interface ResumeHandler {
        /**
         * Called if there are resumable shares.
         * @param ctx     Android application context.
         * @param session The session stored in the resumption data.
         * @param shares  A list of shares stored in the resumption data. Note - these shares do not
         *                have an attached Session; it must be attached using setSession().
         */
        void onSharesFetched(Context ctx, Session session, List<Share> shares);
    }
}
