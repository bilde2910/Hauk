package info.varden.hauk.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Random;

/**
 * A base class for all Hauk notifications. Handles registration into the notification registry and
 * various internally required calls.
 *
 * @author Marius Lindvall
 */
public abstract class HaukNotification {
    private final Context ctx;
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

    // For override by subclasses if necessary.
    @SuppressWarnings({"WeakerAccess", "SameReturnValue"})
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected int getImportance() {
        return NotificationManager.IMPORTANCE_DEFAULT;
    }

    protected abstract void build(NotificationCompat.Builder builder) throws Exception;

    /**
     * Creates a notification instance that can be displayed using NotificationManager.
     *
     * @return A Notification instance.
     * @throws Exception if an exception was thrown during the notification build process.
     */
    public final Notification create() throws Exception {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, NOTIFY_CHANNEL_ID);

        // Pass construction on to the subclass.
        build(builder);

        // On Android >= 8.0, notifications need to be assigned a channel ID.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(NOTIFY_CHANNEL_ID, NOTIFY_CHANNEL_NAME, getImportance());
                NotificationManager nManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                assert nManager != null;
                nManager.createNotificationChannel(channel);
                builder.setChannelId(NOTIFY_CHANNEL_ID);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return builder.build();
    }
}
