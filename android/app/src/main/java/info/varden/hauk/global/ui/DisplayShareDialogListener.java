package info.varden.hauk.global.ui;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.manager.ShareListener;
import info.varden.hauk.struct.Share;

/**
 * Share listener that opens a sharing dialog for the first sharing link received from the server.
 *
 * @author Marius Lindvall
 */
public final class DisplayShareDialogListener implements ShareListener {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * Whether or not the sharing dialog should be displayed.
     */
    private boolean enabled = true;

    public DisplayShareDialogListener(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onShareJoined(Share share) {
        // Ensure that the sharing dialog is only displayed for the first link. The user could
        // otherwise end up having another sharing dialog displayed if the share is adopted and a
        // new sharing link is added that way.
        if (this.enabled) {
            this.enabled = false;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(Constants.INTENT_TYPE_COPY_LINK);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, this.ctx.getString(R.string.share_subject));
            shareIntent.putExtra(Intent.EXTRA_TEXT, share.getViewURL());

            Intent chooserIntent = Intent.createChooser(shareIntent, this.ctx.getString(R.string.share_via));
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.ctx.startActivity(chooserIntent);

            Toast.makeText(this.ctx, R.string.ok_message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onShareParted(Share share) {
    }
}
