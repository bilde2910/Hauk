package info.varden.hauk.caching;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import info.varden.hauk.BuildConfig;
import info.varden.hauk.Constants;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.StringSerializer;

/**
 * If the Hauk app crashes or shuts down, the app should give the option to resume any interrupted
 * shares. This class handles this functionality.
 *
 * @author Marius Lindvall
 */
public final class ResumableSessions {
    private final Context ctx;
    private final SharedPreferences prefs;

    public ResumableSessions(Context ctx) {
        this.ctx = ctx;
        this.prefs = ctx.getSharedPreferences(Constants.SHARED_PREFS_RESUMABLE, Context.MODE_PRIVATE);
    }

    /**
     * If the app crashed, or phone restarted, Hauk gives the option to resume interrupted shares.
     * This function checks if any incomplete shares are saved on the phone and asks the user if
     * they want to resume them.
     *
     * @param handler A handler that is called if there are shares available for resumption.
     */
    public void tryResumeShare(ResumeHandler handler) {
        Log.i("Looking for resumable shares..."); //NON-NLS
        if (this.prefs.getBoolean(Constants.RESUME_AVAILABLE, false)) {
            Log.i("Resumable shares found"); //NON-NLS

            // Check if the app version that wrote the resumption data is the same as this session
            // to avoid deserialization errors due to incompatibilities.
            String writeVersion = this.prefs.getString(Constants.RESUME_CLIENT_VERSION, "");
            if (writeVersion.equals(BuildConfig.VERSION_NAME)) {
                Log.i("Resumable shares are compatible with this version of Hauk"); //NON-NLS

                // Get session parameters.
                Session session = StringSerializer.deserialize(this.prefs.getString(Constants.RESUME_SESSION_PARAMS, null));
                List<Share> shares = StringSerializer.deserialize(this.prefs.getString(Constants.RESUME_SHARE_PARAMS, null));

                // Check that the session is still valid.
                boolean sessionValid = session != null && session.isActive();
                boolean sharesAvailable = shares != null && !shares.isEmpty();

                if (sessionValid && sharesAvailable) {
                    Log.i("Stored session is valid and shares are available"); //NON-NLS
                    handler.onSharesFetched(this.ctx, session, shares.toArray(new Share[0]));
                } else {
                    Log.i("Stored share data is invalid"); //NON-NLS
                    clearResumableSession();
                }
            } else {
                Log.w("Resumption data incompatible, running=%s, stored=%s", BuildConfig.VERSION_NAME, writeVersion); //NON-NLS
            }
        } else {
            Log.i("No resumable shares found"); //NON-NLS
        }
    }

    /**
     * Saves session resumption data. This allows shares to be continued if the app crashes or is
     * otherwise closed.
     *
     * @param session The session to save resumption data for.
     */
    public void setSessionResumable(Session session) {
        Log.i("Setting session %s resumable", session); //NON-NLS
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putBoolean(Constants.RESUME_AVAILABLE, true);
        editor.putString(Constants.RESUME_CLIENT_VERSION, BuildConfig.VERSION_NAME);
        editor.putString(Constants.RESUME_SESSION_PARAMS, StringSerializer.serialize(session));
        editor.apply();
    }

    /**
     * Saves share resumption data. This allows the share to be continued if the app crashes or is
     * otherwise closed.
     *
     * @param share The share to save resumption data for.
     */
    public void setShareResumable(Share share) {
        Log.i("Setting share %s resumable", share); //NON-NLS

        // Get the current list of resumable shares.
        ArrayList<Share> shares = StringSerializer.deserialize(this.prefs.getString(Constants.RESUME_SHARE_PARAMS, null));
        if (shares == null) shares = new ArrayList<>();

        // Add the share and save the updated list.
        shares.add(share);
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(Constants.RESUME_SHARE_PARAMS, StringSerializer.serialize(shares));
        editor.apply();
    }

    /**
     * Removes a share from the list of resumable shares.
     *
     * @param shareID The ID of the share to remove.
     */
    public void clearResumableShare(String shareID) {
        Log.i("Clearing resumable share %s", shareID); //NON-NLS

        // Get the current list of resumable shares.
        ArrayList<Share> shares = StringSerializer.deserialize(this.prefs.getString(Constants.RESUME_SHARE_PARAMS, null));
        if (shares == null) return;

        // Remove the share and save the updated list.
        for (Iterator<Share> it = shares.iterator(); it.hasNext();) {
            if (it.next().getID().equals(shareID)) {
                it.remove();
            }
        }
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.putString(Constants.RESUME_SHARE_PARAMS, StringSerializer.serialize(shares));
        editor.apply();
    }

    /**
     * Clears saved resumable session data.
     */
    public void clearResumableSession() {
        Log.i("Clearing all resumption data"); //NON-NLS

        SharedPreferences.Editor editor = this.prefs.edit();
        editor.clear();
        editor.apply();
    }
}
