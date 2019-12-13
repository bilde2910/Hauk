package info.varden.hauk.global.ui.toast;

import android.content.Context;
import android.widget.Toast;

import info.varden.hauk.R;
import info.varden.hauk.manager.ShareListener;
import info.varden.hauk.struct.Share;

/**
 * Share listener that opens a toast with the first sharing link received from the server.
 *
 * @author Marius Lindvall
 */
public final class ShareListenerImpl implements ShareListener {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * Whether or not the sharing dialog should be displayed.
     */
    private boolean enabled = true;

    public ShareListenerImpl(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onShareJoined(Share share) {
        // Ensure that the toast is only displayed for the first link. The user could otherwise end
        // up having multiple toasts displayed if the share is adopted and a new sharing link is
        // added that way.
        if (this.enabled) {
            this.enabled = false;
            Toast.makeText(this.ctx, String.format(this.ctx.getString(R.string.notify_body), share.getViewURL()), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onShareParted(Share share) {
    }
}
