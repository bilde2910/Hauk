package info.varden.hauk.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Random;

import info.varden.hauk.utils.Log;

/**
 * A base class for all Hauk notifications.
 *
 * @author Marius Lindvall
 */
public abstract class HaukNotification {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * A unique ID used for this notification.
     */
    private final int id;

    @SuppressWarnings("HardCodedStringLiteral")
    private static final String NOTIFY_CHANNEL_ID = "hauk";
    @SuppressWarnings("HardCodedStringLiteral")
    private static final String NOTIFY_CHANNEL_NAME = "Hauk";

    HaukNotification(Context ctx) {
        this.ctx = ctx;

        // Generate a random non-zero ID for the notification.
        Random random = new Random();
        int id;
        do id = random.nextInt(); while (id == 0);
        this.id = id;
    }

    final Context getContext() {
        return this.ctx;
    }

    public final int getID() {
        return this.id;
    }

    /**
     * Called when the notification is being created. Use to build the notification and its
     * contents.
     *
     * @param builder A notification builder instance for the notification being created.
     * @throws Exception if an exception was thrown during the notification build process.
     */
    protected abstract void build(NotificationCompat.Builder builder) throws Exception;

    /**
     * Displays the notification, or updates if it is already displayed.
     */
    final void push() {
        NotificationManager manager = (NotificationManager) this.ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            try {
                manager.notify(this.id, create());
            } catch (Exception e) {
                Log.e("Error while pushing notification", e); //NON-NLS
            }
        } else {
            Log.e("Notification manager is null"); //NON-NLS
        }
    }

    /**
     * Creates a notification instance that can be displayed using NotificationManager.
     *
     * @return A Notification instance.
     * @throws Exception if an exception was thrown during the notification build process.
     */
    public final Notification create() throws Exception {
        Log.d("Creating notification"); //NON-NLS
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.ctx, NOTIFY_CHANNEL_ID);

        // Pass construction on to the subclass.
        build(builder);

        // On Android >= 8.0, notifications need to be assigned a channel ID.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Log.d("Android version O+ detected; setting notification channel"); //NON-NLS
                NotificationChannel channel = new NotificationChannel(NOTIFY_CHANNEL_ID, NOTIFY_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                NotificationManager nManager = (NotificationManager) this.ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                assert nManager != null;
                nManager.createNotificationChannel(channel);
                builder.setChannelId(NOTIFY_CHANNEL_ID);
            } catch (Exception ex) {
                Log.e("Failed to set notification channel; notification may not be displayed", ex); //NON-NLS
            }
        }

        return builder.build();
    }
}
