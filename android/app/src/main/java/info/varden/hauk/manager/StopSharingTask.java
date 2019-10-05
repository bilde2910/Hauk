package info.varden.hauk.manager;

import android.content.Context;
import android.content.Intent;

import info.varden.hauk.http.StopSharingPacket;
import info.varden.hauk.struct.Session;
import info.varden.hauk.utils.Log;

/**
 * This class is a runnable task that will stop location sharing. Only one copy of this task exists
 * in {@link SessionManager}. This class should not be instantiated elsewhere.
 *
 * @author Marius Lindvall
 */
public abstract class StopSharingTask implements Runnable {
    /**
     * Called to clean up the {@link SessionManager}.
     */
    protected abstract void cleanup();

    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * A callback that is called to clean up the {@link SessionManager}.
     */
    private final StopSharingCallback callback;

    /**
     * The task does not have a notification and pusher until updateTask() is called, and the stop
     * task cannot be executed until that is the case. If this task is executable, then location
     * sharing is currently active.
     */
    private boolean canExecute = false;

    /**
     * The location pusher service initiation intent.
     */
    private Intent pusher = null;

    /**
     * If the user stops sharing early, a request should be sent to the server to erase the session.
     * Details about the current session are stored here.
     */
    private Session session = null;

    StopSharingTask(Context ctx, StopSharingCallback callback) {
        this.ctx = ctx;
        this.callback = callback;
    }

    /**
     * Sets the stop task executable. When the task is executed with run(), the provided
     * notification is cleared and the pusher unregistered from the Android location manager.
     *
     * @param pusher A location handler that should be unregistered when sharing is stopped.
     */
    final void updateTask(Intent pusher) {
        Log.i("Setting new update task"); //NON-NLS
        this.pusher = pusher;
        this.canExecute = true;
    }

    /**
     * When a new sharing session is initiated, call this function with the connection settings to
     * register a handler that sends a stop-sharing request to the server when the share should be
     * stopped.
     *
     * @param session The session ID provided by the Hauk backend.
     */
    public final void setSession(Session session) {
        Log.i("Set session %s", session); //NON-NLS
        this.session = session;
    }

    /**
     * Checks whether or not the stop task can be executed. The task is only executable if location
     * sharing is active.
     *
     * @return true if executable, false otherwise.
     */
    final boolean canExecute() {
        return this.canExecute;
    }

    /**
     * Executes the stop task. When run, this will unregister the location handler, clear Hauk's
     * persistent notification, reset the UI to a fresh state and inform the user that sharing has
     * been stopped.
     */
    @Override
    public final void run() {
        if (!this.canExecute) return;
        Log.i("Executing share stop task"); //NON-NLS
        this.canExecute = false;
        Log.i("Stopping location push service"); //NON-NLS
        this.ctx.stopService(this.pusher);

        // If a session is currently active, send a cancellation request to the backend to remove
        // session data from the server.
        if (this.session != null) {
            Log.i("Sending stop packet to server for session %s", this.session); //NON-NLS
            new StopSharingPacket(this.ctx, this.session) {
                @Override
                public void onSuccess() {
                    Log.i("Successfully stopped session"); //NON-NLS
                    cleanup();
                    StopSharingTask.this.callback.onSuccess();
                }

                @Override
                protected void onFailure(Exception ex) {
                    // TODO: Do something meaningful here?
                    Log.e("Failed to stop session", ex); //NON-NLS
                    cleanup();
                    StopSharingTask.this.callback.onFailure(ex);
                }
            }.send();
        } else {
            Log.w("Session is null, cannot stop"); //NON-NLS
            cleanup();
            this.callback.onShareNull();
        }
    }
}
