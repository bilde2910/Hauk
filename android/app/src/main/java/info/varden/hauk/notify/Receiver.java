package info.varden.hauk.notify;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import info.varden.hauk.ReceiverDataRegistry;

/**
 * This class is used to create intents for use in notification buttons that can store an object for
 * retrieval by the associated receiver class. The class maintains a registry of objects for each
 * receiver registered; these objects are returned to the receiver when it is called.
 *
 * @author Marius Lindvall
 * @param <T> The type of data to be passed to the receiving listener.
 */
public class Receiver<T> {
    private final Class<? extends HaukBroadcastReceiver<T>> receiver;
    private final Context ctx;
    private final T data;

    /**
     * Creates a receiver instance.
     *
     * @param ctx      The Android application context.
     * @param receiver The class that Android will instantiate when the proper broadcast is issued.
     * @param data     A data object that will be passed to the broadcast receiver instance.
     */
    public Receiver(Context ctx, Class<? extends HaukBroadcastReceiver<T>> receiver, T data) {
        this.receiver = receiver;
        this.ctx = ctx;
        this.data = data;
    }

    /**
     * Creates a PendingIntent from this receiver. Used to add handlers to notification buttons.
     *
     * @return A PendingIntent for use in a notification action.
     * @throws InstantiationException if the broadcast receiver cannot be instantiated.
     * @throws IllegalAccessException if the broadcast receiver hides the action ID function.
     */
    public PendingIntent toPending() throws InstantiationException, IllegalAccessException {
        // Create a new intent for the receiver.
        Intent intent = new Intent(this.ctx, this.receiver);

        // Retrieve the action ID from the broadcast receiver class.
        HaukBroadcastReceiver<T> instance = this.receiver.newInstance();
        intent.setAction(instance.getActionID());

        // Store the provided data in the registry for later retrieval, and pass the data index to
        // the intent.
        intent.putExtra(Intent.EXTRA_INDEX, ReceiverDataRegistry.register(this.data));

        return PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
