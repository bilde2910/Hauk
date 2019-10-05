package info.varden.hauk.notify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import info.varden.hauk.R;
import info.varden.hauk.utils.Log;

/**
 * A broadcast receiver for the "Copy link" action on the persistent Hauk notification.
 *
 * @author Marius Lindvall
 */
public final class CopyLinkReceiver extends HaukBroadcastReceiver<String> {

    @SuppressWarnings("HardCodedStringLiteral")
    private static final String ACTION_ID = "info.varden.hauk.COPY_LINK";

    public CopyLinkReceiver() {
        super(ACTION_ID);
    }

    @Override
    public void handle(Context ctx, String data) {
        // Copy the link to the clipboard.
        ClipData clip = ClipData.newPlainText(ctx.getString(R.string.action_copied), data);
        ClipboardManager clipMan = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipMan != null) {
            Log.i("Copying %s to clipboard", data); //NON-NLS
            clipMan.setPrimaryClip(clip);
        } else {
            Log.e("Could not copy %s to clipboard because the clipboard manager is null", data); //NON-NLS
        }
    }
}
