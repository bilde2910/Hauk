package info.varden.hauk.ui;

import android.content.Context;
import android.widget.TextView;

import info.varden.hauk.R;
import info.varden.hauk.manager.GNSSStatusUpdateListener;
import info.varden.hauk.utils.Log;

/**
 * Implementation of {@link info.varden.hauk.manager.SessionManager}'s GNSS status update listener
 * for {@link MainActivity}. Updates the given label with the current GNSS status.
 */
final class GNSSStatusLabelUpdater implements GNSSStatusUpdateListener {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * The label to update with the GNSS status.
     */
    private final TextView statusLabel;

    GNSSStatusLabelUpdater(Context ctx, TextView statusLabel) {
        this.ctx = ctx;
        this.statusLabel = statusLabel;
    }

    @Override
    public void onShutdown() {
        Log.d("Resetting GNSS status label"); //NON-NLS
        this.statusLabel.setText(this.ctx.getString(R.string.label_status_none));
        this.statusLabel.setTextColor(this.ctx.getColor(R.color.statusOff));
    }

    @Override
    public void onStarted() {
        Log.d("Set GNSS status label to initial state"); //NON-NLS
        this.statusLabel.setText(this.ctx.getString(R.string.label_status_wait));
        this.statusLabel.setTextColor(this.ctx.getColor(R.color.statusWait));
    }

    @Override
    public void onCoarseLocationReceived() {
        // Indicate to the user that GPS data is being received when the location pusher starts
        // receiving GPS data.
        Log.i("Initial coarse location was received, awaiting high accuracy fix"); //NON-NLS
        this.statusLabel.setText(this.ctx.getString(R.string.label_status_coarse));
    }

    @Override
    public void onAccurateLocationReceived() {
        // Indicate to the user that GPS data is being received when the location pusher starts
        // receiving GPS data.
        Log.i("Initial high accuracy location was received, using GNSS location data for all future location updates"); //NON-NLS
        this.statusLabel.setText(this.ctx.getString(R.string.label_status_ok));
        this.statusLabel.setTextColor(this.ctx.getColor(R.color.statusOn));
    }
}
