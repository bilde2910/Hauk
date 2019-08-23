package info.varden.hauk;

import android.content.Context;
import android.content.Intent;

import java.util.HashMap;

/**
 * This class is a runnable task that will stop location sharing and reset the UI to the state it
 * was in when the app launched. Only one copy of this task exists in the MainActivity class. This
 * class should not be instantiated elsewhere.
 *
 * @author Marius Lindvall
 */
public class StopSharingTask implements Runnable {
    private final Context ctx;
    private final DialogService diagSvc;
    private final Runnable resetTask;

    // The task does not have a notification and pusher until updateTask() is called, and the stop
    // task cannot be executed until that is the case. If this task is executable, then location
    // sharing is currently active.
    private boolean canExecute = false;
    private Intent pusher = null;

    // This task can be run when the activity no longer exists. In that case, do not attempt to
    // reset the UI and show dialogs.
    private boolean activityExists = true;

    // If the user stops sharing early, a request should be sent to the server to erase the session.
    // Store details about the current session here.
    private String baseUrl = null;
    private String session = null;

    protected StopSharingTask(Context ctx, DialogService diagSvc, Runnable resetTask) {
        this.ctx = ctx;
        this.diagSvc = diagSvc;
        this.resetTask = resetTask;
    }

    /**
     * Sets the stop task executable. When the task is executed with run(), the provided
     * notification is cleared and the pusher unregistered from the Android location manager.
     *
     * @param pusher A location handler that should be unregistered when sharing is stopped.
     */
    public void updateTask(Intent pusher) {
        this.pusher = pusher;
        this.canExecute = true;
    }

    /**
     * Informs the stop task that the main activity no longer exists, and that it should not attempt
     * to reset the UI or show dialogs.
     */
    public void setActivityDestroyed() {
        this.activityExists = false;
    }

    /**
     * When a new sharing session is initiated, call this function with the connection settings to
     * register a handler that sends a stop-sharing request to the server when the share should be
     * stopped.
     *
     * @param baseUrl The base URL of the remote Hauk backend.
     * @param session The session ID provided by the Hauk backend.
     */
    public void setSession(String baseUrl, String session) {
        this.baseUrl = baseUrl;
        this.session = session;
    }

    /**
     * Checks whether or not the stop task can be executed. The task is only executable if location
     * sharing is active.
     *
     * @return true if executable, false otherwise.
     */
    public boolean canExecute() {
        return this.canExecute;
    }

    /**
     * Executes the stop task. When run, this will unregister the location handler, clear Hauk's
     * persistent notification, reset the UI to a fresh state and inform the user that sharing has
     * been stopped.
     */
    @Override
    public void run() {
        if (!this.canExecute) return;
        this.canExecute = false;
        this.ctx.stopService(this.pusher);

        // If a session is currently active, send a cancellation request to the backend to remove
        // session data from the server.
        if (this.baseUrl != null && this.session != null) {
            HashMap<String, String> data = new HashMap<>();
            data.put("sid", this.session);
            HTTPThread req = new HTTPThread(new HTTPThread.Callback() {
                @Override
                public void run(HTTPThread.Response resp) {
                    resetApp();
                }
            });
            req.execute(new HTTPThread.Request(this.baseUrl + "api/stop.php", data));
        } else {
            resetApp();
        }
    }

    private void resetApp() {
        if (this.activityExists) {
            this.resetTask.run();
            this.diagSvc.showDialog(R.string.ended_title, R.string.ended_message, this.resetTask);
        } else {
            // If the main activity is already destroyed, there is no reason to keep the app
            // running.
            System.exit(0);
        }
    }
}
