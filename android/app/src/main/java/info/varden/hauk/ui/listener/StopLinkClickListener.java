package info.varden.hauk.ui.listener;

import android.content.Context;
import android.view.View;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.dialog.Buttons;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.dialog.StopSharingConfirmationPrompt;
import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.struct.Share;
import info.varden.hauk.system.preferences.PreferenceManager;
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
     * Service for showing dialogs.
     */
    private final DialogService dialogSvc;

    /**
     * Preference manager, for checking if a confirmation dialog has to be displayed.
     */
    private final PreferenceManager prefs;

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

    public StopLinkClickListener(Context ctx, SessionManager manager, Share share, ShareLinkLayoutManager layout) {
        this.manager = manager;
        this.share = share;
        this.layout = layout;
        this.prefs = new PreferenceManager(ctx);
        this.dialogSvc = new DialogService(ctx);
    }

    @Override
    public void onClick(View view) {
        Log.i("User requested to stop sharing %s", this.share); //NON-NLS
        // If there is only one share still active, stop the entire session rather than just this
        // one share.
        if (this.layout.getShareViewCount() == 1) {
            Log.i("Stopping session because there is only one share left"); //NON-NLS
            if (this.prefs.get(Constants.PREF_CONFIRM_STOP)) {
                this.dialogSvc.showDialog(R.string.dialog_confirm_stop_title, R.string.dialog_confirm_stop_body, Buttons.Three.YES_NO_REMEMBER, new StopSharingConfirmationPrompt(this.prefs, this.manager));
            } else {
                this.manager.stopSharing();
            }
        } else {
            if (this.prefs.get(Constants.PREF_CONFIRM_STOP)) {
                this.dialogSvc.showDialog(R.string.dialog_confirm_stop_title, R.string.dialog_confirm_stop_share, Buttons.Three.YES_NO_REMEMBER, new StopSharingConfirmationPrompt(this.prefs, this.manager, this.share));
            } else {
                this.manager.stopSharing(this.share);
            }
        }
    }
}
