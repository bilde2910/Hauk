package info.varden.hauk.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import info.varden.hauk.Constants;
import info.varden.hauk.http.LocationUpdatePacket;
import info.varden.hauk.http.ServerException;
import info.varden.hauk.http.parameter.LocationProvider;
import info.varden.hauk.manager.StopSharingTask;
import info.varden.hauk.notify.SharingNotification;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.Version;
import info.varden.hauk.system.preferences.PreferenceManager;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.ReceiverDataRegistry;
import info.varden.hauk.utils.TimeUtils;

/**
 * This class is a location listener that POSTs all location updates to Hauk as it receives them. It
 * creates a persistent notification when it launches in order to stay running while the app is
 * minimized.
 *
 * @author Marius Lindvall
 */
public final class LocationPushService extends Service {

    @SuppressWarnings("HardCodedStringLiteral")
    public static final String ACTION_ID = "info.varden.hauk.LOCATION_SERVICE";

    /**
     * A task that should be run when locations start registering. Used further upstream to change a
     * label on the main activity.
     */
    private GNSSActiveHandler gnssActiveTask;

    /**
     * An indicator of whether or the upstream GNSS handler's {@code onCoarseLocationReceived()}
     * callback has been run. This call back should only run once to inform the upstream of the
     * location provider state change when coarse location data has been initially received.
     */
    @SuppressWarnings("BooleanVariableAlwaysNegated")
    private boolean hasRunCoarseTask = false;

    /**
     * An indicator of whether or the upstream GNSS handler's {@code onAccurateLocationReceived()}
     * callback has been run. This call back should only run once to inform the upstream of the
     * location provider state change when fine location data has been initially received.
     */
    @SuppressWarnings("BooleanVariableAlwaysNegated")
    private boolean hasRunAccurateTask = false;

    /**
     * The share that is to be represented in the notification.
     */
    private Share share;

    /**
     * Android location manager instance.
     */
    private LocationManager locMan;

    /**
     * The service's location listener for fine (GNSS, high-accuracy) location updates.
     */
    private FineLocationListener listenFine;

    /**
     * The service's location listener for coarse (network, low-accuracy) location updates.
     */
    private CoarseLocationListener listenCoarse;

    /**
     * The handler that has scheduled the stop task. This is needed so that the callback can be
     * cancelled if the service is relaunched because of a {@link info.varden.hauk.ui.MainActivity}
     * reset/recreation.
     */
    private Handler handler;

    /**
     * Whether or not the last update packet was sent successfully, i.e. whether there is a
     * connection to the backend server.
     */
    private boolean connected = true;

    @Override
    public void onCreate() {
        Log.d("Fetching location service"); //NON-NLS
        this.locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Location push service %s was started, flags=%s, startId=%s", this, flags, startId); //NON-NLS

        // A task that should be run when sharing ends, either automatically or by user request.
        StopSharingTask stopTask = (StopSharingTask) ReceiverDataRegistry.retrieve(intent.getIntExtra(Constants.EXTRA_STOP_TASK, -1));
        this.share = (Share) ReceiverDataRegistry.retrieve(intent.getIntExtra(Constants.EXTRA_SHARE, -1));
        GNSSActiveHandler parentHandler = (GNSSActiveHandler) ReceiverDataRegistry.retrieve(intent.getIntExtra(Constants.EXTRA_GNSS_ACTIVE_TASK, -1));
        this.handler = (Handler) ReceiverDataRegistry.retrieve(intent.getIntExtra(Constants.EXTRA_HANDLER, -1));

        Log.d("Pusher %s was given extras stopTask=%s, share=%s, parentHandler=%s, handler=%s", this, stopTask, this.share, parentHandler, this.handler); //NON-NLS

        try {
            // Even though we previously requested location permission, we still have to check for
            // it when we actually use the location API.
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Log.v("Location permission has been granted"); //NON-NLS
                stopTask.setSession(this.share.getSession());

                // Create a persistent notification for Hauk. This notification does have some
                // buttons that let the user interact with Hauk while in the background, but the
                // real reason we need a notification is so that Android does not kill our app while
                // it is in the background. Having an active notification stops this from happening.
                SharingNotification notify = new SharingNotification(this, this.share, stopTask);
                startForeground(notify.getID(), notify.create());

                // Send status changes both to the parent handler and the notification.
                this.gnssActiveTask = new MultiTargetGNSSHandlerProxy(parentHandler, notify);

                // Create and bind location listeners.
                this.listenCoarse = new CoarseLocationListener();
                this.listenFine = new FineLocationListener();
                if (!this.listenCoarse.request(this.locMan)) this.listenCoarse = null;
                if (!this.listenFine.request(this.locMan)) this.listenFine = null;

            } else {
                Log.e("Location permission that was granted earlier has been rejected - sharing aborted"); //NON-NLS
            }
        } catch (Exception e) {
            Log.e("An exception occurred when starting the location push service", e); //NON-NLS
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (this.listenCoarse != null) {
            Log.i("Service %s destroyed; removing updates from coarse location provider", this); //NON-NLS
            this.locMan.removeUpdates(this.listenCoarse);
        }
        Log.i("Service %s destroyed; removing updates from fine location provider", this); //NON-NLS
        this.listenFine.onStopped();
        this.locMan.removeUpdates(this.listenFine);

        Log.i("Removing callbacks from handler %s", this.handler); //NON-NLS
        this.handler.removeCallbacksAndMessages(null);
        this.gnssActiveTask = new MultiTargetGNSSHandlerProxy();

        Log.i("Stopping foreground service"); //NON-NLS
        stopForeground(true);

        super.onDestroy();
    }

    /**
     * Called when either the coarse or the fine location provider has received a location update.
     * Pushes the location update to the session backend.
     *
     * @param location The location received from the device's location services.
     */
    private void onLocationChanged(Location location, LocationProvider accuracy) {
        Log.v("Sending location update packet"); //NON-NLS
        new LocationUpdatePacketImpl(location, accuracy).send();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Coarse location provider implementation (network-based location).
     */
    private final class CoarseLocationListener extends LocationListenerBase {
        @Override
        public void onLocationChanged(Location location) {
            if (!LocationPushService.this.hasRunCoarseTask) {
                // Notify the main activity that coarse GPS data is now being received,
                // such that the UI can be updated.
                LocationPushService.this.hasRunCoarseTask = true;
                LocationPushService.this.gnssActiveTask.onCoarseLocationReceived();
            }
            Log.v("Location was received on coarse location provider"); //NON-NLS
            LocationPushService.this.onLocationChanged(location, LocationProvider.COARSE);
        }

        @Override
        boolean request(LocationManager manager) throws SecurityException {
            Log.i("Requesting location updates from device location services"); //NON-NLS
            try {
                manager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        LocationPushService.this.share.getSession().getIntervalMillis(),
                        LocationPushService.this.share.getSession().getMinimumDistance(),
                        this
                );
                return true;
            } catch (IllegalArgumentException ex) {
                Log.w("Coarse location provider does not exist!", ex); //NON-NLS
                return false;
            }
        }
    }

    /**
     * Fine location provider implementation (GNSS-based location).
     */
    private final class FineLocationListener extends LocationListenerBase {
        private final Handler noGnssTimer;
        private final PreferenceManager prefs;

        private FineLocationListener() {
            this.noGnssTimer = new Handler();
            this.prefs = new PreferenceManager(LocationPushService.this);
        }

        @Override
        public void onLocationChanged(Location location) {
            if (LocationPushService.this.listenCoarse != null) {
                // Unregister the coarse location listener, since we are now receiving
                // accurate location data.
                Log.i("Accurate location found; removing updates from coarse location provider"); //NON-NLS
                LocationPushService.this.locMan.removeUpdates(LocationPushService.this.listenCoarse);
                LocationPushService.this.listenCoarse = null;
            }
            if (!LocationPushService.this.hasRunAccurateTask) {
                // Notify the main activity that accurate GPS data is now being
                // received, such that the UI can be updated.
                LocationPushService.this.hasRunAccurateTask = true;
                LocationPushService.this.gnssActiveTask.onAccurateLocationReceived();
            }
            Log.v("Location was received on fine location provider"); //NON-NLS

            // Set a timeout for the location updates to detect if the provider stops working. If
            // that happens, fall back to the coarse location provider.
            this.noGnssTimer.removeCallbacksAndMessages(null);
            this.noGnssTimer.postDelayed(new CoarseLocationFallbackTask(), LocationPushService.this.share.getSession().getIntervalMillis() + this.prefs.get(Constants.PREF_NO_GNSS_FALLBACK) * TimeUtils.MILLIS_PER_SECOND);

            LocationPushService.this.onLocationChanged(location, LocationProvider.FINE);
        }

        /**
         * Should be called when the session is stopped and updates removed from this listener. This
         * prevents the timeout from activating after the session has been stopped.
         */
        private void onStopped() {
            this.noGnssTimer.removeCallbacksAndMessages(false);
        }

        @Override
        boolean request(LocationManager manager) throws SecurityException {
            manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LocationPushService.this.share.getSession().getIntervalMillis(),
                    LocationPushService.this.share.getSession().getMinimumDistance(),
                    this
            );
            return true;
        }

        private final class CoarseLocationFallbackTask implements Runnable {
            @Override
            public void run() {
                // No location updates have been received for the timeout period. Rebind the coarse
                // location listener while we wait for the fine listener to become functional again.
                Log.w("Location fix lost. Rebinding coarse location provider."); //NON-NLS
                LocationPushService.this.gnssActiveTask.onCoarseRebound();
                LocationPushService.this.hasRunCoarseTask = false;
                LocationPushService.this.hasRunAccurateTask = false;
                LocationPushService.this.listenCoarse = new CoarseLocationListener();
                if (!LocationPushService.this.listenCoarse.request(LocationPushService.this.locMan)) {
                    LocationPushService.this.listenCoarse = null;
                }
            }
        }
    }

    private final class LocationUpdatePacketImpl extends LocationUpdatePacket {
        private LocationUpdatePacketImpl(Location location, LocationProvider accuracy) {
            super(LocationPushService.this, LocationPushService.this.share.getSession(), location, accuracy);
        }

        @Override
        public void onShareListReceived(String linkFormat, String[] shares) {
            Log.v("Received list of shares from server"); //NON-NLS
            LocationPushService.this.gnssActiveTask.onShareListReceived(linkFormat, shares);
        }

        @Override
        protected void onSuccess(String[] data, Version backendVersion) throws ServerException {
            // Check if connection was lost previously, and notify upstream if that's the case.
            if (!LocationPushService.this.connected) {
                LocationPushService.this.connected = true;
                Log.i("Connection to the backend was restored."); //NON-NLS
                LocationPushService.this.gnssActiveTask.onServerConnectionRestored();
            }
            super.onSuccess(data, backendVersion);
        }

        @Override
        protected void onFailure(Exception ex) {
            Log.w("Failed to push location update to server", ex); //NON-NLS
            // Notify upstream about connectivity loss.
            if (LocationPushService.this.connected) {
                LocationPushService.this.connected = false;
                Log.i("Connection to the backend was lost."); //NON-NLS
                LocationPushService.this.gnssActiveTask.onServerConnectionLost();
            }
        }
    }
}
