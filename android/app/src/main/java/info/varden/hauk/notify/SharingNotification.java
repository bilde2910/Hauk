package info.varden.hauk.notify;

import android.content.Context;

import androidx.core.app.NotificationCompat;

import info.varden.hauk.R;
import info.varden.hauk.StopSharingTask;
import info.varden.hauk.struct.Share;
import info.varden.hauk.ui.MainActivity;

/**
 * Hauk's persistent notification that prevents Hauk from being stopped while in the background.
 *
 * @author Marius Lindvall
 */
public class SharingNotification extends HaukNotification {
    // The share that this notification represents.
    private final Share share;

    // A task to be executed when sharing stops. In the case of this notification, it is executed if
    // the user taps the "Stop sharing" button on the notification.
    private final StopSharingTask stopSharingTask;

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
    }

    @Override
    public void build(NotificationCompat.Builder builder) throws Exception {
        builder.setContentTitle(getContext().getString(R.string.notify_title));
        builder.setContentText(String.format(getContext().getString(R.string.notify_body), this.share.getSession().getServerURL()));
        builder.setSmallIcon(R.drawable.ic_notify);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Add "Copy link" and "Stop sharing" buttons to the notification.
        builder.addAction(R.drawable.ic_notify, getContext().getString(R.string.action_copy), new Receiver<>(getContext(), CopyLinkReceiver.class, this.share.getViewURL()).toPending());
        builder.addAction(R.drawable.ic_notify, getContext().getString(R.string.action_stop), new Receiver<>(getContext(), StopSharingReceiver.class, this.stopSharingTask).toPending());
        builder.setContentIntent(new ReopenIntent(getContext(), MainActivity.class).toPending());

        builder.setOngoing(true);
    }
}
