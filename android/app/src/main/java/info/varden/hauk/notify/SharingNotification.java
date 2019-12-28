package info.varden.hauk.notify;

import android.content.Context;

import androidx.core.app.NotificationCompat;

import info.varden.hauk.R;
import info.varden.hauk.manager.StopSharingTask;
import info.varden.hauk.service.GNSSActiveHandler;
import info.varden.hauk.struct.Share;
import info.varden.hauk.ui.MainActivity;
import info.varden.hauk.utils.Log;

/**
 * Hauk's persistent notification that prevents Hauk from being stopped while in the background.
 *
 * @author Marius Lindvall
 */
public final class SharingNotification extends HaukNotification implements GNSSActiveHandler {
    /**
     * The share that this notification represents.
     */
    private final Share share;

    /**
     * A task to be executed when sharing stops. In the case of this notification, it is executed if
     * the user taps the "Stop sharing" button on the notification.
     */
    private final StopSharingTask stopSharingTask;

    /**
     * A string resource representing the title currently displayed in the notification.
     */
    private int notifyTitle;

    /**
     * The old notification title, if switching to or from the "backend connection lost" title.
     */
    private int lastTitle;

    /**
     * Creates a persistent notification.
     *
     * @param ctx             Android application context.
     * @param share           The share represented by this notification.
     * @param stopSharingTask A task to run if the user stops sharing their location.
     */
    public SharingNotification(Context ctx, Share share, StopSharingTask stopSharingTask) {
        super(ctx);
        this.share = share;
        this.stopSharingTask = stopSharingTask;
        this.notifyTitle = R.string.label_status_wait;
        this.lastTitle = R.string.label_status_wait;
    }

    @Override
    public void build(NotificationCompat.Builder builder) throws Exception {
        Log.v("Building sharing notification"); //NON-NLS
        builder.setContentTitle(getContext().getString(this.notifyTitle));
        builder.setContentText(String.format(getContext().getString(R.string.notify_body), this.share.getSession().getServerURL()));
        builder.setSmallIcon(R.drawable.ic_notify);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Add "Copy link" and "Stop sharing" buttons to the notification.
        builder.addAction(R.drawable.ic_button_copy, getContext().getString(R.string.action_copy), new Receiver<>(getContext(), CopyLinkReceiver.class, this.share.getViewURL()).toPending());
        builder.addAction(R.drawable.ic_button_stop, getContext().getString(R.string.action_stop), new Receiver<>(getContext(), StopSharingReceiver.class, this.stopSharingTask).toPending());
        builder.setContentIntent(new ReopenIntent(getContext(), MainActivity.class).toPending());

        builder.setOngoing(true);
    }

    @Override
    public void onCoarseRebound() {
        this.notifyTitle = R.string.label_status_lost_gnss;
        this.lastTitle = this.notifyTitle;
        push();
    }

    @Override
    public void onCoarseLocationReceived() {
        this.notifyTitle = R.string.label_status_coarse;
        this.lastTitle = this.notifyTitle;
        push();
    }

    @Override
    public void onAccurateLocationReceived() {
        this.notifyTitle = R.string.label_status_ok;
        this.lastTitle = this.notifyTitle;
        push();
    }

    @Override
    public void onServerConnectionLost() {
        this.notifyTitle = R.string.label_status_disconnected;
        push();
    }

    @Override
    public void onServerConnectionRestored() {
        this.notifyTitle = this.lastTitle;
        push();
    }

    @Override
    public void onShareListReceived(String linkFormat, String[] shareIDs) {
    }
}
