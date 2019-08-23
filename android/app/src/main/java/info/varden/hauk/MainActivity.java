package info.varden.hauk;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import info.varden.hauk.service.LocationPushService;

/**
 * The main activity for Hauk.
 *
 * @author Marius Lindvall
 */
public class MainActivity extends AppCompatActivity {

    // UI elements on activity_main.xml
    private EditText txtServer;
    private EditText txtPassword;
    private EditText txtDuration;
    private EditText txtInterval;
    private Button btnShare;
    private Button btnLink;

    // The publicly sharable link received from the Hauk server during handshake
    private String viewLink;

    // A helper utility class for displaying dialog windows/message boxes.
    private DialogService diagSvc;

    // A runnable task that is executed when location sharing stops. It clears the persistent Hauk
    // notification, unregisters the location pusher and resets the UI to a fresh state.
    private StopSharingTask stopTask;

    // A runnable task that resets the UI to a fresh state.
    private Runnable resetTask;

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setClassVariables();
        loadPreferences();
    }

    @Override
    protected void onDestroy() {
        stopTask.setActivityDestroyed();
        super.onDestroy();
    }

    /**
     * On-tap handler for the "start sharing" and "stop sharing" button.
     */
    public void startSharing(View view) {
        // If there is an executable stop task, that means that sharing is already active. Shut down
        // the share by running the stop task instead of starting a new share.
        if (stopTask.canExecute()) {
            stopTask.run();
            return;
        }

        // Temporarily disable the share button while we attempt to connect to the Hauk backend.
        btnShare.setEnabled(false);

        String server = txtServer.getText().toString();
        final String password = txtPassword.getText().toString();
        int duration = Integer.parseInt(txtDuration.getText().toString());
        final int interval = Integer.parseInt(txtInterval.getText().toString());

        // Save connection preferences for next launch, so the user doesn't have to enter URL etc.
        // every time.
        setPreferences(server, duration, interval);

        // Create a "full" server address, with a following slash if it is missing. This is used to
        // construct subpaths for the Hauk backend.
        final String serverFull = server.endsWith("/") ? server : server + "/";

        // The backend takes duration in seconds, so convert the minutes supplied by the user.
        final int durationSec = duration * 60;

        // Check for location permission and prompt the user if missing. This returns because the
        // checking function creates async dialogs here - the user is prompted to press the button
        // again instead.
        if (!hasLocationPermission()) return;

        final LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = false;
        try {
            isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {};
        if (!isGPSEnabled) {
            diagSvc.showDialog(R.string.err_client, R.string.err_location_disabled, resetTask);
            return;
        }

        // Create a progress dialog while doing initial handshake. This could end up taking a while
        // (e.g. if the host is unreachable, it will eventually time out), and having a progress bar
        // makes for better UX since it visually shows that something is actually happening in the
        // background.
        final ProgressDialog prog = new ProgressDialog(this);
        prog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        prog.setTitle(R.string.prog_title);
        prog.setMessage(getString(R.string.prog_body));
        prog.setIndeterminate(true);
        prog.setCancelable(false);
        prog.show();

        // Create a handshake request and handle the response. The handshake transmits the duration
        // and interval to the server and waits for the server to return a session ID to confirm
        // session creation.
        HashMap<String, String> data = new HashMap<>();
        data.put("pwd", password);
        data.put("dur", String.valueOf(durationSec));
        data.put("int", String.valueOf(interval));
        HTTPThread req = new HTTPThread(new HTTPThread.Callback() {
            @Override
            public void run(HTTPThread.Response resp) {
                prog.dismiss();

                // An exception may have occurred, but it cannot be thrown because this is a
                // callback. Instead, the exception (if any) is stored in the response object.
                Exception e = resp.getException();
                if (e == null) {

                    // A successful session initiation contains "OK" on line 1, the session ID on
                    // line 2, and a publicly sharable tracking link on line 3.
                    String[] data = resp.getData();
                    if (data[0].equals("OK")) {
                        String session = data[1];
                        viewLink = data[2];

                        // We now have a link to share, so we enable the link sharing button.
                        btnLink.setEnabled(true);

                        // Even though we previously requested location permission, we still have to
                        // check for it when we actually use the location API (user could have
                        // disabled it while connecting).
                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                            // Create a client that receives location updates and pushes these to
                            // the Hauk backend.
                            Intent pusher = new Intent(MainActivity.this, LocationPushService.class);
                            pusher.setAction(LocationPushService.ACTION_ID);
                            pusher.putExtra("baseUrl", serverFull);
                            pusher.putExtra("viewUrl", viewLink);
                            pusher.putExtra("session", session);
                            pusher.putExtra("interval", (long) interval * 1000L);
                            pusher.putExtra("stopTask", ReceiverDataRegistry.register(stopTask));
                            if (Build.VERSION.SDK_INT >= 26) {
                                startForegroundService(pusher);
                            } else {
                                startService(pusher);
                            }

                            // When both the notification and pusher are created, we can update the
                            // stop task with these so that they can be canceled when the location
                            // share ends.
                            stopTask.updateTask(pusher);
                            final Handler handler = new Handler();

                            // stopTask is scheduled for expiration, but it could also be called if
                            // the user manually stops the share, or if the app is destroyed.
                            handler.postDelayed(stopTask, durationSec * 1000L);

                            // Now that sharing is finally active, we can re-enable the start
                            // button, turn it into a stop button, and inform the user.
                            btnShare.setEnabled(true);
                            btnShare.setText(R.string.btn_stop);
                            diagSvc.showDialog(R.string.ok_title, R.string.ok_message, null);
                        } else {
                            diagSvc.showDialog(R.string.err_client, R.string.err_missing_perms, resetTask);
                        }
                    } else {
                        // If the first line of the response is not "OK", an error of some sort has
                        // occurred and should be displayed to the user.
                        StringBuilder err = new StringBuilder();
                        for (String line : data) {
                            err.append(line);
                            err.append("\n");
                        }
                        diagSvc.showDialog(R.string.err_server, err.toString(), resetTask);
                    }
                } else if (e instanceof MalformedURLException) {
                    e.printStackTrace();
                    diagSvc.showDialog(R.string.err_client, R.string.err_malformed_url, resetTask);
                } else if (e instanceof IOException) {
                    e.printStackTrace();
                    diagSvc.showDialog(R.string.err_connect, e.getMessage(), resetTask);
                } else {
                    e.printStackTrace();
                    diagSvc.showDialog(R.string.err_server, e.getMessage(), resetTask);
                }
            }
        });
        req.execute(new HTTPThread.Request(serverFull + "api/create.php", data));
    }

    /**
     * On-tap handler for the "share link" button. Opens a share menu.
     */
    public void shareLink(View view) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, viewLink);
        startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_via)));
    }

    /**
     * Checks whether or not the user granted Hauk permission to use their device location. If
     * permission has not been granted, this function creates a dialog which runs asynchronously,
     * meaning this function does not wait until permission has been granted before it returns.
     *
     * @return true if permission is granted, false if the user needs to be asked.
     */
    private boolean hasLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Show a rationale first before requesting location permission, giving users the chance
            // to cancel the request if they so desire. Users are informed that they must click the
            // "start sharing" button again after they have granted the permission.
            diagSvc.showDialog(R.string.req_perms_title, R.string.req_perms_message, new Runnable() {

                /**
                 * Function that runs if the user accepts the location request rationale via the
                 * OK button.
                 */
                @Override
                public void run() {
                    btnShare.setEnabled(true);
                    btnLink.setEnabled(false);
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                }
            }, new Runnable() {

                /**
                 * Function that runs if the user accepts the location request rationale via the
                 * Cancel button.
                 */
                @Override
                public void run() {
                    btnShare.setEnabled(true);
                    btnLink.setEnabled(false);
                }
            });
            return false;
        } else {
            return true;
        }
    }

    /**
     * This function is called by onCreate() to initialize class-level variables for usage in this
     * activity.
     */
    private void setClassVariables() {
        txtServer = findViewById(R.id.txtServer);
        txtPassword = findViewById(R.id.txtPassword);
        txtDuration = findViewById(R.id.txtDuration);
        txtInterval = findViewById(R.id.txtInterval);
        btnShare = findViewById(R.id.btnShare);
        btnLink = findViewById(R.id.btnLink);

        resetTask = new Runnable() {

            /**
             * A function which resets the user interface to its default settings, as if the app was
             * just opened. Used to reset the UI after errors and after sharing has expired.
             */
            @Override
            public void run() {
                btnShare.setEnabled(true);
                btnShare.setText(R.string.btn_start);
                btnLink.setEnabled(false);
            }
        };

        diagSvc = new DialogService(this);
        stopTask = new StopSharingTask(this, diagSvc, resetTask);
    }

    private void loadPreferences() {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("connectionPrefs", MODE_PRIVATE);
        txtServer.setText(settings.getString("server", ""));
        txtDuration.setText(String.valueOf(settings.getInt("duration", 30)));
        txtInterval.setText(String.valueOf(settings.getInt("interval", 1)));
    }

    private void setPreferences(String server, int duration, int interval) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("connectionPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("server", server);
        editor.putInt("duration", duration);
        editor.putInt("interval", interval);
        editor.apply();
    }
}
