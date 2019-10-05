package info.varden.hauk.notify;

import android.content.Context;

import info.varden.hauk.manager.StopSharingTask;
import info.varden.hauk.utils.Log;

/**
 * A broadcast receiver for the "Stop sharing" button on the persistent Hauk notification.
 *
 * @author Marius Lindvall
 */
public final class StopSharingReceiver extends HaukBroadcastReceiver<StopSharingTask> {

    @SuppressWarnings("HardCodedStringLiteral")
    private static final String ACTION_ID = "info.varden.hauk.STOP_SHARING";

    public StopSharingReceiver() {
        super(ACTION_ID);
    }

    @Override
    public void handle(Context ctx, StopSharingTask data) {
        // Run the stop sharing task to end location sharing.
        Log.i("User requested to stop sharing via notification (broadcast)"); //NON-NLS
        data.run();
    }
}
