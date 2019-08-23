package info.varden.hauk.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import java.util.HashMap;

import info.varden.hauk.HTTPThread;
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
public class LocationPushService extends Service implements LocationListener {

    public static final String ACTION_ID = "info.varden.hauk.LOCATION_SERVICE";

    // The base URL of the Hauk server.
    private String baseUrl;
    // The publicly sharable link for the current share.
    private String viewUrl;
    // A task that should be run when sharing ends, either automatically or by user request.
    private StopSharingTask stopTask;

    private String session;
    private long interval;

    private LocationManager locMan;

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

                locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, this.interval, 0F, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        locMan.removeUpdates(this);
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
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
                // The response to this HTTP request can be ignored - there is no need for two-way
                // communication in this case, as the pusher is only meant to push data.
            }
        });
        req.execute(new HTTPThread.Request(this.baseUrl + "api/post.php", data));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {}

    @Override
    public void onProviderDisabled(String s) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
