package info.varden.hauk.notify;

import android.content.Context;

import info.varden.hauk.StopSharingTask;

/**
 * A broadcast receiver for the "Stop sharing" button on the persistent Hauk notification.
 *
 * @author Marius Lindvall
 */
public class StopSharingReceiver extends HaukBroadcastReceiver<StopSharingTask> {

    public static final String ACTION_ID = "info.varden.hauk.STOP_SHARING";

    @Override
    public String getActionID() {
        return ACTION_ID;
    }

    @Override
    public void handle(Context ctx, StopSharingTask data) {
        // Run the stop sharing task to end location sharing.
        data.run();
    }
}
