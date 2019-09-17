package info.varden.hauk.notify;

import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import info.varden.hauk.MainActivity;
import info.varden.hauk.R;
import info.varden.hauk.StopSharingTask;

/**
 * Hauk's persistent notification that prevents Hauk from being stopped while in the background.
 *
 * @author Marius Lindvall
 */
public class SharingNotification extends HaukNotification {
    // The Hauk backend base URL e.g. https://example.com/.
    private final String baseUrl;

    // The publicly sharable URL for this share, e.g. https://example.com?ABCD-1234.
    private final String viewUrl;

    // A task to be executed when sharing stops. In the case of this notification, it is executed if
    // the user taps the "Stop sharing" button on the notification.
    private final StopSharingTask stopSharingTask;

    /**
     * Creates a persistent notification.
     *
     * @param ctx             Android application context.
     * @param baseUrl         The Hauk backend base URL.
     * @param viewUrl         The publicly sharable link for this share.
     * @param stopSharingTask A task to run if the user stops sharing their location.
     */
    public SharingNotification(Context ctx, String baseUrl, String viewUrl, StopSharingTask stopSharingTask) {
        super(ctx);
        this.baseUrl = baseUrl;
        this.viewUrl = viewUrl;
        this.stopSharingTask = stopSharingTask;
    }

    @Override
    public int getImportance() {
        return NotificationManager.IMPORTANCE_DEFAULT;
    }

    @Override
    public void build(NotificationCompat.Builder builder) throws Exception {
        builder.setContentTitle(getContext().getString(R.string.notify_title));
        builder.setContentText(String.format(getContext().getString(R.string.notify_body), baseUrl));
        builder.setSmallIcon(R.drawable.ic_notify);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Add "Copy link" and "Stop sharing" buttons to the notification.
        builder.addAction(R.drawable.ic_notify, getContext().getString(R.string.action_copy), new Receiver<>(getContext(), CopyLinkReceiver.class, this.viewUrl).toPending());
        builder.addAction(R.drawable.ic_notify, getContext().getString(R.string.action_stop), new Receiver<>(getContext(), StopSharingReceiver.class, this.stopSharingTask).toPending());
        builder.setContentIntent(new ReopenIntent(getContext(), MainActivity.class).toPending());

        builder.setOngoing(true);
    }
}
