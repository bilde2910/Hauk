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
public class ReopenIntent {
    private final Context ctx;
    private final Class<? extends Activity> activity;

    public ReopenIntent(Context ctx, Class <? extends Activity> activity) {
        this.ctx = ctx;
        this.activity = activity;
    }

    public PendingIntent toPending() {
        Intent intent = new Intent(this.ctx, this.activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return PendingIntent.getActivity(this.ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
