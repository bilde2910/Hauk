package info.varden.hauk.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import info.varden.hauk.utils.ReceiverDataRegistry;

/**
 * A superclass for broadcast receivers, used together with the Receiver class to handle callbacks
 * from users clicking buttons in Hauk notifications. Passes the data object from Receiver to the
 * subclass of this class for processing.
 *
 * @author Marius Lindvall
 * @param <T> The type of data this broadcast receiver is capable of processing.
 */
public abstract class HaukBroadcastReceiver<T> extends BroadcastReceiver {
    /**
     * A function that provides a unique broadcast action ID that this receiver should handle.
     *
     * @return A broadcast activity ID.
     */
    public abstract String getActionID();

    /**
     * Callback for handling the broadcast data.
     * @param ctx  Android application context.
     * @param data The data object provided to the Receiver class.
     */
    public abstract void handle(Context ctx, T data);

    @Override
    public final void onReceive(Context ctx, Intent intent) {
        // Retrieve the registry index of the data stored for this receiver, then pass that data on
        // to the subclass.
        int index = intent.getIntExtra(Intent.EXTRA_INDEX, -1);
        handle(ctx, (T) ReceiverDataRegistry.retrieve(index));
    }
}
