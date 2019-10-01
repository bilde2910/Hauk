package info.varden.hauk.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;

import info.varden.hauk.HaukConst;
import info.varden.hauk.StopSharingTask;
import info.varden.hauk.http.LocationUpdatePacket;
import info.varden.hauk.notify.HaukNotification;
import info.varden.hauk.notify.SharingNotification;
import info.varden.hauk.struct.Share;
import info.varden.hauk.utils.ReceiverDataRegistry;

/**
 * This class is a location listener that POSTs all location updates to Hauk as it receives them. It
 * creates a persistent notification when it launches in order to stay running while the app is
 * minimized.
 *
 * @author Marius Lindvall
 */
public class LocationPushService extends Service {

    @SuppressWarnings("HardCodedStringLiteral")
    public static final String ACTION_ID = "info.varden.hauk.LOCATION_SERVICE";

    // A task that should be run when locations start registering. Used to change a label on the
    // main activity.
    private GNSSActiveHandler gnssActiveTask;
    private boolean hasRunCoarseTask = false;
    private boolean hasRunAccurateTask = false;

    // The share that is to be represented in the notification.
    private Share share;

    private LocationManager locMan;

    private LocationListener listenFine;
    private LocationListener listenCoarse;

    /**
     * Called when the Service is created.
     */
    @Override
    public void onCreate() {
        this.locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // A task that should be run when sharing ends, either automatically or by user request.
        StopSharingTask stopTask = (StopSharingTask) ReceiverDataRegistry.retrieve(intent.getIntExtra(HaukConst.EXTRA_STOP_TASK, -1));
        this.share = (Share) ReceiverDataRegistry.retrieve(intent.getIntExtra(HaukConst.EXTRA_SHARE, -1));
        this.gnssActiveTask = (GNSSActiveHandler) ReceiverDataRegistry.retrieve(intent.getIntExtra(HaukConst.EXTRA_GNSS_ACTIVE_TASK, -1));

        try {
            // Even though we previously requested location permission, we still have to check for
            // it when we actually use the location API.
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                stopTask.setSession(this.share.getSession());

                // Create a persistent notification for Hauk. This notification does have some
                // buttons that let the user interact with Hauk while in the background, but the
                // real reason we need a notification is so that Android does not kill our app while
                // it is in the background. Having an active notification stops this from happening.
                final HaukNotification notify = new SharingNotification(this, this.share, stopTask);
                startForeground(notify.getID(), notify.create());

                this.listenCoarse = new LocationListenerBase() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (!hasRunCoarseTask) {
                            // Notify the main activity that coarse GPS data is now being received,
                            // such that the UI can be updated.
                            hasRunCoarseTask = true;
                            gnssActiveTask.onCoarseLocationReceived();
                        }
                        LocationPushService.this.onLocationChanged(location);
                    }
                };

                this.listenFine = new LocationListenerBase() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (listenCoarse != null) {
                            // Unregister the coarse location listener, since we are now receiving
                            // accurate location data.
                            locMan.removeUpdates(listenCoarse);
                            listenCoarse = null;
                        }
                        if (!hasRunAccurateTask) {
                            // Notify the main activity that accurate GPS data is now being
                            // received, such that the UI can be updated.
                            hasRunAccurateTask = true;
                            gnssActiveTask.onAccurateLocationReceived();
                        }
                        LocationPushService.this.onLocationChanged(location);
                    }
                };

                locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, this.share.getSession().getIntervalMillis(), 0F, this.listenCoarse);
                locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, this.share.getSession().getIntervalMillis(), 0F, this.listenFine);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (this.listenCoarse != null) this.locMan.removeUpdates(this.listenCoarse);
        this.locMan.removeUpdates(this.listenFine);
        stopForeground(true);
        super.onDestroy();
    }

    private void onLocationChanged(Location location) {
        new LocationUpdatePacket(this, this.share.getSession(), location) {
            @Override
            public void onShareListReceived(String linkFormat, String[] shares) {
                gnssActiveTask.onShareListReceived(linkFormat, shares);
            }

            @Override
            protected void onFailure(Exception ex) {
                // Errors can be due to intermittent connectivity. Ignore them.
            }
        }.send();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
