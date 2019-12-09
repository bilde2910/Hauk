package info.varden.hauk.ui.listener;

import android.view.View;

import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.struct.Share;
import info.varden.hauk.ui.ShareLinkLayoutManager;
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
     * Link layout on the UI.
     */
    private final ShareLinkLayoutManager layout;

    /**
     * The share to share the link for.
     */
    private final Share share;

    public StopLinkClickListener(SessionManager manager, Share share, ShareLinkLayoutManager layout) {
        this.manager = manager;
        this.share = share;
        this.layout = layout;
    }

    @Override
    public void onClick(View view) {
        Log.i("User requested to stop sharing %s", this.share); //NON-NLS
        // If there is only one share still active, stop the entire session rather than just this
        // one share.
        if (this.layout.getShareViewCount() == 1) {
            Log.i("Stopping session because there is only one share left"); //NON-NLS
            this.manager.stopSharing();
        } else {
            this.manager.stopSharing(this.share);
        }
    }
}
