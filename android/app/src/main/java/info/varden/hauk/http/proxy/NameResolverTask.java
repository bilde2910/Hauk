package info.varden.hauk.http.proxy;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import java.net.InetSocketAddress;
import java.net.Proxy;

import info.varden.hauk.Constants;
import info.varden.hauk.http.FailureHandler;
import info.varden.hauk.system.preferences.PreferenceManager;
import info.varden.hauk.system.preferences.indexresolver.ProxyTypeResolver;
import info.varden.hauk.utils.Log;

/**
 * Async task that checks whether or not a proxy address must be resolved, resolves it if necessary,
 * and passes execution back to callbacks. This is necessary because proxy hostname resolution is
 * network-dependent and therefore not permitted on the UI thread.
 *
 * @author Marius Lindvall
 */
public abstract class NameResolverTask extends AsyncTask<Void, Void, Proxy> implements FailureHandler {
    /**
     * Called if hostname resolution is required.
     *
     * @param hostname The hostname of the proxy.
     */
    protected abstract void onResolutionStarted(String hostname);

    /**
     * Called if the hostname could not be resolved. This is a failure state.
     *
     * @param hostname The hostname that could not be resolved.
     */
    protected abstract void onHostUnresolved(String hostname);

    /**
     * Called if proxy hostname resolution was required and was successful, or if resolution was
     * skipped because no proxy is in use.
     *
     * @param proxy A proxy that can be used when connecting to the backend. May be null if no proxy
     *              should be used.
     */
    protected abstract void onSuccess(@Nullable Proxy proxy);

    private final Proxy.Type proxyType;
    private final String proxyHost;
    private final int proxyPort;

    private boolean wasSuccessful = true;

    protected NameResolverTask(PreferenceManager prefs) {
        this.proxyType = ProxyTypeResolver.fromIndex(prefs.get(Constants.PREF_PROXY_TYPE)).getProxyType();
        this.proxyHost = prefs.get(Constants.PREF_PROXY_HOST).trim();
        this.proxyPort = prefs.get(Constants.PREF_PROXY_PORT);
    }

    /**
     * Starts the proxy resolution process.
     */
    public final void resolve() {
        if (this.proxyType == Proxy.Type.DIRECT) {
            // If explicitly using no proxy, forward the NO_PROXY upstream.
            Log.i("Using direct connection to backend server"); //NON-NLS
            onSuccess(Proxy.NO_PROXY);

        } else if (this.proxyType == null) {
            // If system default is set, forward null proxy upstream.
            Log.i("Using system default proxy to backend server"); //NON-NLS
            onSuccess(null);

        } else {
            // Otherwise, a proxy is in use and a hostname may need to be resolved.
            Log.i("Using a proxy; resolving the hostname for %s", this.proxyHost); //NON-NLS
            onResolutionStarted(this.proxyHost);
            this.execute();
        }
    }

    @Nullable
    @Override
    protected final Proxy doInBackground(Void... params) {
        try {
            Log.i("Resolving proxy hostname %s...", this.proxyHost); //NON-NLS
            InetSocketAddress proxyAddr = new InetSocketAddress(this.proxyHost, this.proxyPort);

            // Check if the hostname could be resolved.
            if (proxyAddr.isUnresolved()) {
                Log.e("Proxy hostname %s is unresolved", this.proxyHost); //NON-NLS
                this.wasSuccessful = false;
                onHostUnresolved(this.proxyHost);
                return null;
            } else {
                Log.v("Proxy hostname resolution was successful!"); //NON-NLS
                return new Proxy(this.proxyType, proxyAddr);
            }

        } catch (Exception ex) {
            Log.e("Proxy setup failed for proxy %s:%d", ex, this.proxyHost, this.proxyPort); //NON-NLS
            this.wasSuccessful = false;
            onFailure(ex);
            return null;
        }
    }

    @Override
    protected final void onPostExecute(Proxy result) {
        if (this.wasSuccessful) onSuccess(result);
    }
}
