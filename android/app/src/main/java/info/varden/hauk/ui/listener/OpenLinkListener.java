package info.varden.hauk.ui.listener;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.preference.Preference;

public final class OpenLinkListener implements Preference.OnPreferenceClickListener {
    private final Context ctx;
    private final Uri uri;

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
}
