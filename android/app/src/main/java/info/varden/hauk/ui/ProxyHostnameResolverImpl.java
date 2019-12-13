package info.varden.hauk.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.net.Proxy;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.dialog.Buttons;
import info.varden.hauk.dialog.CustomDialogBuilder;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.http.ConnectionParameters;
import info.varden.hauk.http.SessionInitiationPacket;
import info.varden.hauk.http.proxy.NameResolverTask;
import info.varden.hauk.manager.SessionInitiationResponseHandler;
import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.struct.AdoptabilityPreference;
import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.system.LocationPermissionsNotGrantedException;
import info.varden.hauk.system.LocationServicesDisabledException;
import info.varden.hauk.system.preferences.PreferenceManager;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.TimeUtils;

/**
 * Implementation of {@link NameResolverTask} for {@link MainActivity}. This implementation is
 * responsible for starting shares after the proxy configuration has been resolved.
 */
public final class ProxyHostnameResolverImpl extends NameResolverTask {
    private final PreferenceManager prefs;
    private final WeakReference<Activity> ctx;
    private final SessionManager manager;
    private final Runnable uiResetTask;
    private final SessionInitiationResponseHandler responseHandler;
    private final ShareMode mode;
    private final SessionInitiationPacket.InitParameters initParams;
    private final boolean allowAdoption;
    private final String nickname;
    private final String groupPin;

    private final Object progressLock = new Object();
    private ProgressDialog progress = null;

    ProxyHostnameResolverImpl(Activity ctx, SessionManager manager, Runnable uiResetTask, PreferenceManager prefs, SessionInitiationResponseHandler responseHandler, SessionInitiationPacket.InitParameters initParams, ShareMode mode, boolean allowAdoption, String nickname, String groupPin) {
        super(prefs);
        this.prefs = prefs;
        this.ctx = new WeakReference<>(ctx);
        this.manager = manager;
        this.uiResetTask = uiResetTask;
        this.responseHandler = responseHandler;
        this.initParams = initParams;
        this.mode = mode;
        this.allowAdoption = allowAdoption;
        this.nickname = nickname;
        this.groupPin = groupPin;
    }

    @Override
    protected void onResolutionStarted(String hostname) {
        // Show a progress dialog only if resolution has to take place. Otherwise, no progress
        // dialog is shown.
        synchronized (this.progressLock) {
            this.progress = new ProgressDialog(this.ctx.get());
            this.progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            this.progress.setTitle(R.string.progress_connect_title);
            this.progress.setMessage(this.ctx.get().getString(R.string.progress_resolving_proxy));
            this.progress.setIndeterminate(true);
            this.progress.setCancelable(false);
            this.progress.show();
        }
    }

    @Override
    protected void onHostUnresolved(final String hostname) {
        // The hostname couldn't be resolved. Show an error message.
        final Activity ctx = this.ctx.get();
        if (ctx != null) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Hide progress dialog if visible.
                    synchronized (ProxyHostnameResolverImpl.this.progressLock) {
                        if (ProxyHostnameResolverImpl.this.progress != null) {
                            ProxyHostnameResolverImpl.this.progress.dismiss();
                        }
                    }
                    new DialogService(ctx).showDialog(R.string.err_connect, ctx.getString(R.string.err_proxy_host_resolution, hostname), ProxyHostnameResolverImpl.this.uiResetTask);
                }
            });
        }
    }

    @Override
    protected void onSuccess(@Nullable Proxy proxy) {
        // Hide progress dialog if visible.
        synchronized (this.progressLock) {
            if (this.progress != null) {
                this.progress.dismiss();
            }
        }

        // Set the proxy.
        int timeout = this.prefs.get(Constants.PREF_CONNECTION_TIMEOUT) * (int) TimeUtils.MILLIS_PER_SECOND;
        ConnectionParameters params;
        if (proxy == null) {
            params = new ConnectionParameters(null, null, timeout);
        } else {
            params = new ConnectionParameters(proxy.type(), proxy.address(), timeout);
        }
        this.initParams.setConnectionParameters(params);

        // Start the location share.
        try {
            switch (this.mode) {
                case CREATE_ALONE:
                    this.manager.shareLocation(this.initParams, this.responseHandler, this.allowAdoption ? AdoptabilityPreference.ALLOW_ADOPTION : AdoptabilityPreference.DISALLOW_ADOPTION);
                    break;

                case CREATE_GROUP:
                    this.manager.shareLocation(this.initParams, this.responseHandler, this.nickname);
                    break;

                case JOIN_GROUP:
                    this.manager.shareLocation(this.initParams, this.responseHandler, this.nickname, this.groupPin);
                    break;

                default:
                    Log.wtf("Unknown sharing mode. This is not supposed to happen, ever"); //NON-NLS
                    break;
            }
        } catch (LocationServicesDisabledException e) {
            Log.e("Share initiation was stopped because location services are disabled", e); //NON-NLS
            final Context ctx = this.ctx.get();
            if (ctx != null) {
                new DialogService(ctx).showDialog(R.string.err_client, R.string.err_location_disabled, Buttons.SETTINGS_OK, new CustomDialogBuilder() {
                    @Override
                    public void onPositive() {
                        // OK button
                        ProxyHostnameResolverImpl.this.uiResetTask.run();
                    }

                    @Override
                    public void onNegative() {
                        // Open Settings button
                        ProxyHostnameResolverImpl.this.uiResetTask.run();
                        ctx.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }

                    @Nullable
                    @Override
                    public View createView(Context ctx) {
                        return null;
                    }
                });
            }
        } catch (LocationPermissionsNotGrantedException e) {
            Log.w("Share initiation was stopped because the user has not granted location permissions yet", e); //NON-NLS
        }
    }

    @Override
    public void onFailure(final Exception ex) {
        // Proxy configuration failed for some reason. Show the error message to the user in a
        // dialog.
        final Activity ctx = this.ctx.get();
        if (ctx != null) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Hide progress dialog if visible.
                    synchronized (ProxyHostnameResolverImpl.this.progressLock) {
                        if (ProxyHostnameResolverImpl.this.progress != null) {
                            ProxyHostnameResolverImpl.this.progress.dismiss();
                        }
                    }
                    new DialogService(ctx).showDialog(R.string.err_connect, ctx.getString(R.string.err_proxy_failure, ex.getLocalizedMessage()), ProxyHostnameResolverImpl.this.uiResetTask);
                }
            });
        }
    }
}
