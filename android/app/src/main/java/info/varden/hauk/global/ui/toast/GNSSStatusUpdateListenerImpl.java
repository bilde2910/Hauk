package info.varden.hauk.global.ui.toast;

import android.content.Context;
import android.widget.Toast;

import info.varden.hauk.R;
import info.varden.hauk.manager.GNSSStatusUpdateListener;

/**
 * GNSS status update listener that displays status updates in toast notifications. Used primarily
 * for starting shares via the broadcast receiver.
 *
 * @author Marius Lindvall
 */
public final class GNSSStatusUpdateListenerImpl implements GNSSStatusUpdateListener {
    /**
     * Android application context.
     */
    private final Context ctx;

    public GNSSStatusUpdateListenerImpl(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onShutdown() {
        Toast.makeText(this.ctx, R.string.label_status_none, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStarted() {
        Toast.makeText(this.ctx, R.string.label_status_wait, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCoarseLocationReceived() {
        Toast.makeText(this.ctx, R.string.label_status_coarse, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAccurateLocationReceived() {
        Toast.makeText(this.ctx, R.string.label_status_ok, Toast.LENGTH_LONG).show();
    }
}
