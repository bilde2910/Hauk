package info.varden.hauk.ui.listener;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import info.varden.hauk.R;
import info.varden.hauk.struct.Share;
import info.varden.hauk.utils.Log;

/**
 * On-click listener for the button that opens a share menu for a link in the list of active links
 * on the UI, spawned using ShareLinkLayoutManager.
 *
 * @see info.varden.hauk.ui.MainActivity
 * @author Marius Lindvall
 */
public final class ShareLinkClickListener implements View.OnClickListener {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * The share to share the link for.
     */
    private final Share share;

    public ShareLinkClickListener(Context ctx, Share share) {
        this.ctx = ctx;
        this.share = share;
    }

    @Override
    public void onClick(View view) {
        Log.i("User requested to share %s", this.share); //NON-NLS
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        //noinspection HardCodedStringLiteral
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, this.ctx.getString(R.string.share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, this.share.getViewURL());
        this.ctx.startActivity(Intent.createChooser(shareIntent, this.ctx.getString(R.string.share_via)));
    }
}
