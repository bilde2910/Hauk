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

import java.util.HashMap;

import info.varden.hauk.HTTPThread;
import info.varden.hauk.HaukConst;
import info.varden.hauk.ReceiverDataRegistry;
import info.varden.hauk.StopSharingTask;
import info.varden.hauk.notify.SharingNotification;

/**
 * This class is a location listener that POSTs all location updates to Hauk as it receives them. It
 * creates a persistent notification when it launches in order to stay running while the app is
 * minimized.
 *
 * @author Marius Lindvall
 */
public class LocationPushService extends Service {

    public static final String ACTION_ID = "info.varden.hauk.LOCATION_SERVICE";

    // The base URL of the Hauk server.
    private String baseUrl;
    // The publicly sharable link for the current share.
    private String viewUrl;
    // A task that should be run when sharing ends, either automatically or by user request.
    private StopSharingTask stopTask;
    // A task that should be run when locations start registering. Used to change a label on the
    // main activity.
    private GNSSActiveHandler gnssActiveTask;
    private boolean hasRunCoarseTask = false;
    private boolean hasRunAccurateTask = false;

    private String session;
    private long interval;
    private String[] shares;

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
        this.baseUrl = intent.getStringExtra("baseUrl");
        this.viewUrl = intent.getStringExtra("viewUrl");
        this.session = intent.getStringExtra("session");
        this.interval = intent.getLongExtra("interval", -1L);
        this.stopTask = (StopSharingTask) ReceiverDataRegistry.retrieve(intent.getIntExtra("stopTask", -1));
        this.gnssActiveTask = (GNSSActiveHandler) ReceiverDataRegistry.retrieve(intent.getIntExtra("gnssActiveTask", -1));
        this.shares = new String[0];

        try {
            // Even though we previously requested location permission, we still have to check for
            // it when we actually use the location API.
            if (this.interval >= 0L && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                this.stopTask.setSession(this.baseUrl, this.session);

                // Create a persistent notification for Hauk. This notification does have some
                // buttons that let the user interact with Hauk while in the background, but the
                // real reason we need a notification is so that Android does not kill our app while
                // it is in the background. Having an active notification stops this from happening.
                final SharingNotification notify = new SharingNotification(this, this.baseUrl, this.viewUrl, this.stopTask);
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

                locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, this.interval, 0F, this.listenCoarse);
                locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, this.interval, 0F, this.listenFine);
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
        HashMap<String, String> data = new HashMap<>();
        data.put("lat", String.valueOf(location.getLatitude()));
        data.put("lon", String.valueOf(location.getLongitude()));
        data.put("time", String.valueOf((double) System.currentTimeMillis() / 1000D));
        data.put("sid", session);
        if (location.hasSpeed()) data.put("spd", String.valueOf(location.getSpeed()));
        if (location.hasAccuracy()) data.put("acc", String.valueOf(location.getAccuracy()));
        HTTPThread req = new HTTPThread(new HTTPThread.Callback() {
            @Override
            public void run(HTTPThread.Response resp) {
                if (resp.getException() == null) {
                    String[] data = resp.getData();
                    if (data[0].equals("OK")) {

                        // If the backend is >= v1.2, post.php returns a list of currently active
                        // share links. Update the user interface to include these.
                        if (resp.getServerVersion().atLeast(HaukConst.VERSION_COMPAT_VIEW_ID)) {

                            // The share link list is comma-separated.
                            String shareCSV = data[2];
                            String[] shares = new String[0];
                            if (shareCSV.length() > 0) {
                                shares = shareCSV.split(",");
                            }
                            gnssActiveTask.onShareListReceived(data[1], shares);
                        }
                    }
                }
            }
        });
        req.execute(new HTTPThread.Request(this.baseUrl + "api/post.php", data));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
