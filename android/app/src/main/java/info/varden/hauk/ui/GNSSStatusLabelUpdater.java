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

    private int lastStatus = R.string.label_status_none;
    private int lastColor = R.color.statusOff;

    GNSSStatusLabelUpdater(Context ctx, TextView statusLabel) {
        this.ctx = ctx;
        this.statusLabel = statusLabel;
    }

    @Override
    public void onShutdown() {
        Log.d("Resetting GNSS status label"); //NON-NLS
        this.statusLabel.setText(R.string.label_status_none);
        this.statusLabel.setTextColor(this.ctx.getColor(R.color.statusOff));
        this.lastStatus = R.string.label_status_none;
        this.lastColor = R.color.statusOff;
    }

    @Override
    public void onStarted() {
        Log.d("Set GNSS status label to initial state"); //NON-NLS
        this.statusLabel.setText(R.string.label_status_wait);
        this.statusLabel.setTextColor(this.ctx.getColor(R.color.statusWait));
        this.lastStatus = R.string.label_status_wait;
        this.lastColor = R.color.statusWait;
    }

    @Override
    public void onGNSSConnectionLost() {
        // Indicate to the user that the GNSS connection was lost, and that we are now searching for
        // a location again.
        Log.i("GNSS location provider has stopped working; bound to coarse location provider"); //NON-NLS
        this.statusLabel.setText(R.string.label_status_lost_gnss);
        this.statusLabel.setTextColor(this.ctx.getColor(R.color.statusWait));
        this.lastStatus = R.string.label_status_lost_gnss;
        this.lastColor = R.color.statusWait;
    }

    @Override
    public void onCoarseLocationReceived() {
        // Indicate to the user that GPS data is being received when the location pusher starts
        // receiving GPS data.
        Log.i("Initial coarse location was received, awaiting high accuracy fix"); //NON-NLS
        this.statusLabel.setText(R.string.label_status_coarse);
        this.statusLabel.setTextColor(this.ctx.getColor(R.color.statusWait));
        this.lastStatus = R.string.label_status_coarse;
        this.lastColor = R.color.statusWait;
    }

    @Override
    public void onAccurateLocationReceived() {
        // Indicate to the user that GPS data is being received when the location pusher starts
        // receiving GPS data.
        Log.i("Initial high accuracy location was received, using GNSS location data for all future location updates"); //NON-NLS
        this.statusLabel.setText(R.string.label_status_ok);
        this.statusLabel.setTextColor(this.ctx.getColor(R.color.statusOn));
        this.lastStatus = R.string.label_status_ok;
        this.lastColor = R.color.statusOn;
    }

    @Override
    public void onServerConnectionLost() {
        // Indicate to the user that the backend connection was lost.
        this.statusLabel.setText(R.string.label_status_disconnected);
        this.statusLabel.setTextColor(this.ctx.getColor(R.color.statusDisconnected));
    }

    @Override
    public void onServerConnectionRestored() {
        // Restore the previous status when connection to the backend is restored.
        this.statusLabel.setText(this.lastStatus);
        this.statusLabel.setTextColor(this.ctx.getColor(this.lastColor));
    }
}
