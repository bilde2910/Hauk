package info.varden.hauk.ui.listener;

import android.view.View;

import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.struct.Share;
import info.varden.hauk.utils.Log;

/**
 * On-click listener for the button that stops sharing for a link in the list of active links on the
 * UI, spawned using ShareLinkLayoutManager.
 *
 * @see info.varden.hauk.ui.MainActivity
 * @author Marius Lindvall
 */
public final class StopLinkClickListener implements View.OnClickListener {
    /**
     * Android application context.
     */
    private final SessionManager manager;

    /**
     * The share to share the link for.
     */
    private final Share share;

    public StopLinkClickListener(SessionManager manager, Share share) {
        this.manager = manager;
        this.share = share;
    }

    @Override
    public void onClick(View view) {
        Log.i("User requested to stop sharing %s", this.share); //NON-NLS
        this.manager.stopSharing(this.share);
    }
}
