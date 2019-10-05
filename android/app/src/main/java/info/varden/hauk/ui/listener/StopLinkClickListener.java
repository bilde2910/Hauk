package info.varden.hauk.ui.listener;

import android.content.Context;
import android.view.View;

import info.varden.hauk.http.StopSharingPacket;
import info.varden.hauk.struct.Share;
import info.varden.hauk.utils.Log;

/**
 * On-click listener for the button that stops sharing for a link in the list of active links on the
 * UI, spawned using ShareLinkLayoutManager.
 *
 * @see info.varden.hauk.ui.MainActivity
 * @author Marius Lindvall
 */
public final class StopLinkClickListener implements View.OnClickListener {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * The share to share the link for.
     */
    private final Share share;

    public StopLinkClickListener(Context ctx, Share share) {
        this.ctx = ctx;
        this.share = share;
    }

    @Override
    public void onClick(View view) {
        Log.i("User requested to stop sharing %s", this.share); //NON-NLS
        new AssociatedPacket().send();
    }

    /**
     * The packet sent to the server to request that sharing is stopped.
     */
    private final class AssociatedPacket extends StopSharingPacket {
        private AssociatedPacket() {
            super(StopLinkClickListener.this.ctx, StopLinkClickListener.this.share);
        }

        @Override
        public void onSuccess() {
            // TODO: Do something meaningful here?
            Log.i("Share %s was successfully stopped", StopLinkClickListener.this.share); //NON-NLS
        }

        @Override
        protected void onFailure(Exception ex) {
            Log.e("Share %s could not be stopped", ex, StopLinkClickListener.this.share); //NON-NLS
        }
    }
}
