package info.varden.hauk.global;

import android.content.Context;
import android.widget.Toast;

import info.varden.hauk.R;
import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.manager.StopSharingCallback;

/**
 * Session manager implementation for sessions created via the broadcast receiver.
 *
 * @author Marius Lindvall
 */
public final class BroadcastSessionManager extends SessionManager {
    /**
     * Android application context.
     */
    private final Context ctx;

    BroadcastSessionManager(Context ctx) {
        super(ctx, new StopSharingCallbackImpl(ctx));
        this.ctx = ctx;
    }

    @Override
    protected void requestLocationPermission() {
        Toast.makeText(this.ctx, R.string.err_missing_perms, Toast.LENGTH_LONG).show();
    }

    /**
     * Implementation of {@link StopSharingCallback} for the broadcast receiver session manager.
     * Displays toast notifications when sharing stops.
     */
    private static final class StopSharingCallbackImpl implements StopSharingCallback {
        private final Context ctx;

        private StopSharingCallbackImpl(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        public void onSuccess() {
            Toast.makeText(this.ctx, R.string.ended_message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onShareNull() {
        }

        @Override
        public void onFailure(Exception ex) {
            Toast.makeText(this.ctx, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
