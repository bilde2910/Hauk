package info.varden.hauk.global.ui.toast;

import android.content.Context;
import android.widget.Toast;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.manager.SessionInitiationResponseHandler;
import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.struct.Version;

/**
 * Session initiation response handler that shows the initiation status in toasts. Used primarily
 * for shares created via the broadcast receiver.
 */
public final class SessionInitiationResponseHandlerImpl implements SessionInitiationResponseHandler {
    /**
     * Android application context.
     */
    private final Context ctx;

    public SessionInitiationResponseHandlerImpl(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onInitiating() {
    }

    @Override
    public void onSuccess() {
    }

    @Override
    public void onShareModeForciblyDowngraded(ShareMode downgradeTo, Version backendVersion) {
        Toast.makeText(this.ctx, String.format(this.ctx.getString(R.string.err_ver_group), Constants.VERSION_COMPAT_GROUP_SHARE, backendVersion), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFailure(Exception ex) {
        Toast.makeText(this.ctx, ex.getMessage(), Toast.LENGTH_LONG).show();
    }
}
