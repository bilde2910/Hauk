package info.varden.hauk.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import info.varden.hauk.Constants;
import info.varden.hauk.http.LocationUpdatePacket;
import info.varden.hauk.http.ServerException;
import info.varden.hauk.manager.StopSharingTask;
import info.varden.hauk.notify.HaukNotification;
import info.varden.hauk.notify.SharingNotification;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.Version;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.ReceiverDataRegistry;

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
    private LocationListener listenFine;

    /**
     * The service's location listener for coarse (network, low-accuracy) location updates.
     */
    private LocationListener listenCoarse;

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
        Log.i("Location push service was started, flags=%s, startId=%s", flags, startId); //NON-NLS

        // A task that should be run when sharing ends, either automatically or by user request.
        StopSharingTask stopTask = (StopSharingTask) ReceiverDataRegistry.retrieve(intent.getIntExtra(Constants.EXTRA_STOP_TASK, -1));
        this.share = (Share) ReceiverDataRegistry.retrieve(intent.getIntExtra(Constants.EXTRA_SHARE, -1));
        this.gnssActiveTask = (GNSSActiveHandler) ReceiverDataRegistry.retrieve(intent.getIntExtra(Constants.EXTRA_GNSS_ACTIVE_TASK, -1));
        this.handler = (Handler) ReceiverDataRegistry.retrieve(intent.getIntExtra(Constants.EXTRA_HANDLER, -1));

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
                HaukNotification notify = new SharingNotification(this, this.share, stopTask);
                startForeground(notify.getID(), notify.create());

                this.listenCoarse = new LocationListenerBase() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (!LocationPushService.this.hasRunCoarseTask) {
                            // Notify the main activity that coarse GPS data is now being received,
                            // such that the UI can be updated.
                            LocationPushService.this.hasRunCoarseTask = true;
                            LocationPushService.this.gnssActiveTask.onCoarseLocationReceived();
                        }
                        Log.v("Location was received on coarse location provider"); //NON-NLS
                        LocationPushService.this.onLocationChanged(location);
                    }
                };

                this.listenFine = new LocationListenerBase() {
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
                        LocationPushService.this.onLocationChanged(location);
                    }
                };

                attachToLocationServices();
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
            Log.i("Service destroyed; removing updates from coarse location provider"); //NON-NLS
            this.locMan.removeUpdates(this.listenCoarse);
        }
        Log.i("Service destroyed; removing updates from fine location provider"); //NON-NLS
        this.locMan.removeUpdates(this.listenFine);

        Log.i("Removing callbacks from handler"); //NON-NLS
        this.handler.removeCallbacksAndMessages(null);

        Log.i("Stopping foreground service"); //NON-NLS
        stopForeground(true);

        super.onDestroy();
    }

    /**
     * Attaches the listeners to the location manager to request updates.
     *
     * @throws SecurityException If location permission is missing.
     */
    private void attachToLocationServices() throws SecurityException {
        Log.i("Requesting location updates from device location services"); //NON-NLS
        try {
            this.locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, this.share.getSession().getIntervalMillis(), this.share.getSession().getMinimumDistance(), this.listenCoarse);
        } catch (IllegalArgumentException ex) {
            Log.w("Coarse location provider does not exist!", ex); //NON-NLS
            this.listenCoarse = null;
        }
        this.locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, this.share.getSession().getIntervalMillis(), this.share.getSession().getMinimumDistance(), this.listenFine);
    }

    /**
     * Called when either the coarse or the fine location provider has received a location update.
     * Pushes the location update to the session backend.
     *
     * @param location The location received from the device's location services.
     */
    private void onLocationChanged(Location location) {
        Log.v("Sending location update packet"); //NON-NLS
        new LocationUpdatePacketImpl(location).send();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class LocationUpdatePacketImpl extends LocationUpdatePacket {
        private LocationUpdatePacketImpl(Location location) {
            super(LocationPushService.this, LocationPushService.this.share.getSession(), location);
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
