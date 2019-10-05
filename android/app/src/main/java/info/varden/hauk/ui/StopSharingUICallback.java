package info.varden.hauk.ui;

import android.content.Context;

import info.varden.hauk.R;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.manager.StopSharingCallback;
import info.varden.hauk.utils.Log;

/**
 * Callback that is called to reset the UI when sharing stops.
 *
 * @see MainActivity
 * @author Marius Lindvall
 */
final class StopSharingUICallback implements StopSharingCallback {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * Task that resets the UI to a default state.
     */
    private final Runnable resetTask;

    /**
     * This task can be run when the activity no longer exists. In that case, do not attempt to
     * reset the UI and show dialogs.
     */
    private boolean activityExists = true;

    StopSharingUICallback(Context ctx, Runnable uiResetTask) {
        this.ctx = ctx;
        this.resetTask = uiResetTask;
    }

    /**
     * Informs the stop task that the main activity no longer exists, and that it should not attempt
     * to reset the UI or show dialogs.
     */
    void setActivityDestroyed() {
        Log.i("Main activity flagged as destroyed"); //NON-NLS
        this.activityExists = false;
    }

    /**
     * Resets the app UI to its default state and shows a dialog informing the user that the sharing
     * session has ended.
     */
    private void resetApp() {
        if (this.activityExists) {
            this.resetTask.run();
            new DialogService(this.ctx).showDialog(R.string.ended_title, R.string.ended_message, this.resetTask);
        } else {
            // If the main activity is already destroyed, there is no reason to keep the app
            // running.
            Log.i("Main activity no longer exists; exiting"); //NON-NLS
            System.exit(0);
        }
    }

    @Override
    public void onSuccess() {
        resetApp();
    }

    @Override
    public void onFailure(Exception ex) {
        resetApp();
    }

    @Override
    public void onShareNull() {
        resetApp();
    }
}
