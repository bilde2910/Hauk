package info.varden.hauk.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import info.varden.hauk.Constants;
import info.varden.hauk.utils.Log;
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
    private final String actionID;

    /**
     * Creates the receiver.
     *
     * @param actionID A unique broadcast action ID that this receiver should handle.
     */
    protected HaukBroadcastReceiver(String actionID) {
        this.actionID = actionID;
    }

    /**
     * Callback for handling the broadcast data.
     *
     * @param ctx  Android application context.
     * @param data The data object provided to the Receiver class.
     */
    protected abstract void handle(Context ctx, T data);

    public final String getActionID() {
        return this.actionID;
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        // Retrieve the registry index of the data stored for this receiver, then pass that data on
        // to the subclass.
        int index = intent.getIntExtra(Constants.EXTRA_BROADCAST_RECEIVER_REGISTRY_INDEX, -1);
        //noinspection unchecked
        T data = (T) ReceiverDataRegistry.retrieve(index, true);
        Log.v("Received broadcast for class %s; fetched stored data of type %s; calling handler", getClass().getName(), data.getClass().getName()); //NON-NLS
        handle(context, data);
    }
}
