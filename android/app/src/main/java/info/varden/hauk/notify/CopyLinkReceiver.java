package info.varden.hauk.notify;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import info.varden.hauk.R;

/**
 * A broadcast receiver for the "Copy link" action on the persistent Hauk notification.
 *
 * @author Marius Lindvall
 */
public class CopyLinkReceiver extends HaukBroadcastReceiver<String> {

    @SuppressWarnings("HardCodedStringLiteral")
    private static final String ACTION_ID = "info.varden.hauk.COPY_LINK";

    @Override
    public String getActionID() {
        return ACTION_ID;
    }

    @Override
    public void handle(Context ctx, String data) {
        // Copy the link to the clipboard.
        try {
            ClipData clip = ClipData.newPlainText(ctx.getString(R.string.action_copied), data);
            ((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }
}
