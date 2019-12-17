package info.varden.hauk.system.launcher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.preference.Preference;

/**
 * Listener that opens a URI in the browser on click.
 *
 * @author Marius Lindvall
 */
public final class OpenLinkListener implements Preference.OnPreferenceClickListener, View.OnClickListener {
    private final Context ctx;
    private final Uri uri;

    /**
     * Creates the click listener.
     *
     * @param ctx         Android application context.
     * @param uriResource A string resource representing the link to open.
     */
    public OpenLinkListener(Context ctx, int uriResource) {
        this(ctx, Uri.parse(ctx.getString(uriResource)));
    }

    private OpenLinkListener(Context ctx, Uri uri) {
        this.ctx = ctx;
        this.uri = uri;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        this.ctx.startActivity(new Intent(Intent.ACTION_VIEW, this.uri));
        return false;
    }

    @Override
    public void onClick(View view) {
        this.ctx.startActivity(new Intent(Intent.ACTION_VIEW, this.uri));
    }
}
