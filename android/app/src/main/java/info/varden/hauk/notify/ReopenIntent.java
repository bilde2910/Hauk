package info.varden.hauk.notify;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * A intent for tapping the persistent Hauk notification to return to the app.
 *
 * @author Marius Lindvall
 */
final class ReopenIntent {
    private final Context ctx;
    private final Class<? extends Activity> activity;

    /**
     * Creates an intent that can be used to return to the app when the notification is tapped.
     *
     * @param ctx      Android application context.
     * @param activity The class of the activity to return to.
     */
    ReopenIntent(Context ctx, @SuppressWarnings("SameParameterValue") Class <? extends Activity> activity) {
        this.ctx = ctx;
        this.activity = activity;
    }

    /**
     * Creates an intent for the reopen task and converts it to a PendingIntent for use in
     * notification builders.
     */
    PendingIntent toPending() {
        Intent intent = new Intent(this.ctx, this.activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return PendingIntent.getActivity(this.ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
