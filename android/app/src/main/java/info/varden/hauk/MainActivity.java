package info.varden.hauk;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import info.varden.hauk.dialog.AdoptDialogBuilder;
import info.varden.hauk.dialog.DialogService;
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
    private EditText txtNickname;
    private EditText txtPIN;
    private Spinner selMode;
    private Button btnShare;
    private Button btnLink;
    private TextView labelStatusCur;
    private CheckBox chkRemember;
    private TextView labelAdoptWhatsThis;
    private CheckBox chkAllowAdopt;
    private TableRow rowAllowAdopt;
    private TableRow rowNickname;
    private TableRow rowPIN;

    private LinearLayout layoutGroupPIN;
    private TextView labelShowPin;

    // The publicly sharable link received from the Hauk server during handshake
    private String viewLink;

    // A helper utility class for displaying dialog windows/message boxes.
    private DialogService diagSvc;

    // A runnable task that is executed when location sharing stops. It clears the persistent Hauk
    // notification, unregisters the location pusher and resets the UI to a fresh state.
    private StopSharingTask stopTask;

    // A timer that counts down the number of seconds left of the share period.
    private Timer shareCountdown;

    // A runnable task that resets the UI to a fresh state.
    private Runnable resetTask;

    // A dialog builder that can build a dialog used to adopt existing single-user shares into a
    // group share.
    private AdoptDialogBuilder adoptBuilder;

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setClassVariables();
        loadPreferences();

        labelAdoptWhatsThis.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        // Add an on checked handler to the password remember checkbox to save their password
        // immediately.
        chkRemember.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                if (checked) {
                    setPassword(true, txtPassword.getText().toString());
                } else {
                    setPassword(false, "");
                }
            }
        });

        // Add elements and an event handler to the sharing mode selector.
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sel_mode_opts, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selMode.setAdapter(adapter);
        selMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int selection, long rowId) {
                // This handler determines which UI elements should be visible, based on the user's
                // selection of sharing modes.
                switch (selection) {
                    case HaukConst.SHARE_MODE_CREATE_ALONE:
                        rowAllowAdopt.setVisibility(View.VISIBLE);
                        rowNickname.setVisibility(View.GONE);
                        rowPIN.setVisibility(View.GONE);
                        break;
                    case HaukConst.SHARE_MODE_CREATE_GROUP:
                        rowAllowAdopt.setVisibility(View.GONE);
                        rowNickname.setVisibility(View.VISIBLE);
                        rowPIN.setVisibility(View.GONE);
                        break;
                    case HaukConst.SHARE_MODE_JOIN_GROUP:
                        rowAllowAdopt.setVisibility(View.GONE);
                        rowNickname.setVisibility(View.VISIBLE);
                        rowPIN.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
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

        // Disable the UI while we attempt to connect to the Hauk backend.
        btnShare.setEnabled(false);
        txtServer.setEnabled(false);
        txtPassword.setEnabled(false);
        txtDuration.setEnabled(false);
        txtInterval.setEnabled(false);

        selMode.setEnabled(false);
        txtNickname.setEnabled(false);
        txtPIN.setEnabled(false);
        chkAllowAdopt.setEnabled(false);

        String server = txtServer.getText().toString().trim();
        final String password = txtPassword.getText().toString();
        int duration = Integer.parseInt(txtDuration.getText().toString());
        final int interval = Integer.parseInt(txtInterval.getText().toString());
        final String nickname = txtNickname.getText().toString().trim();
        final int shareMode = selMode.getSelectedItemPosition();
        final String groupPin = txtPIN.getText().toString();
        final boolean allowAdoption = chkAllowAdopt.isChecked();

        // Save connection preferences for next launch, so the user doesn't have to enter URL etc.
        // every time.
        setPreferences(server, duration, interval, nickname, allowAdoption);

        // If password saving is enabled, save the password as well.
        if (chkRemember.isChecked()) setPassword(true, password);

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
        data.put("mod", String.valueOf(shareMode));
        switch (shareMode) {
            case HaukConst.SHARE_MODE_CREATE_ALONE:
                data.put("ado", allowAdoption ? "1" : "0");
                break;
            case HaukConst.SHARE_MODE_CREATE_GROUP:
                data.put("nic", nickname);
                break;
            case HaukConst.SHARE_MODE_JOIN_GROUP:
                data.put("nic", nickname);
                data.put("pin", groupPin);
                break;
        }
        HTTPThread req = new HTTPThread(new HTTPThread.Callback() {
            @Override
            public void run(HTTPThread.Response resp) {
                int actualShareMode = shareMode;
                prog.dismiss();

                // An exception may have occurred, but it cannot be thrown because this is a
                // callback. Instead, the exception (if any) is stored in the response object.
                Exception e = resp.getException();
                if (e == null) {

                    // Check if the server is out of date for group shares, if applicable.
                    if (actualShareMode == HaukConst.SHARE_MODE_CREATE_GROUP || actualShareMode == HaukConst.SHARE_MODE_JOIN_GROUP) {
                        if (resp.getServerVersion().olderThan(HaukConst.VERSION_COMPAT_GROUP_SHARE)) {
                            // If the server is indeed out of date, override the sharing mode to
                            // reflect what was actually created on the server.
                            actualShareMode = HaukConst.SHARE_MODE_CREATE_ALONE;
                            selMode.setSelection(HaukConst.SHARE_MODE_CREATE_ALONE);
                            diagSvc.showDialog(R.string.err_outdated, String.format(getString(R.string.err_ver_group), HaukConst.VERSION_COMPAT_GROUP_SHARE, resp.getServerVersion()));
                        }
                    }

                    // A successful session initiation contains "OK" on line 1, the session ID on
                    // line 2, and a publicly sharable tracking link on line 3.
                    final String[] data = resp.getData();

                    // Somehow the data array is empty.
                    if (data.length < 1) {
                        diagSvc.showDialog(R.string.err_server, R.string.err_empty, resetTask);
                        return;
                    }

                    if (data[0].equals("OK")) {
                        String session = data[1];
                        viewLink = data[2];
                        if (actualShareMode == HaukConst.SHARE_MODE_CREATE_GROUP) {
                            final String joinCode = data[3];

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    // Show the group PIN on the UI if a new group share was created.
                                    labelShowPin.setText(joinCode);
                                    layoutGroupPIN.setVisibility(View.VISIBLE);
                                }
                            });

                            // Create a new adoption dialog builder for this group share.
                            adoptBuilder = new AdoptDialogBuilder(MainActivity.this, serverFull, session, joinCode) {
                                @Override
                                public void onSuccess(final String nick) {
                                    diagSvc.showDialog(R.string.adopted_title, String.format(getString(R.string.adopted_body), nick));
                                }

                                @Override
                                public void onFailure(final Exception ex) {
                                    diagSvc.showDialog(R.string.err_server, ex.getMessage());
                                }
                            };
                        }

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
                            pusher.putExtra("gnssActiveTask", ReceiverDataRegistry.register(new Runnable() {

                                @Override
                                public void run() {
                                    // Indicate to the user that GPS data is being received when the
                                    // location pusher starts receiving GPS data.
                                    labelStatusCur.setText(getString(R.string.label_status_ok));
                                    labelStatusCur.setTextColor(getColor(R.color.statusOn));
                                }
                            }));
                            if (Build.VERSION.SDK_INT >= 26) {
                                startForegroundService(pusher);
                            } else {
                                startService(pusher);
                            }

                            // When both the notification and pusher are created, we can update the
                            // stop task with these so that they can be canceled when the location
                            // share ends.
                            stopTask.updateTask(pusher);

                            // stopTask is scheduled for expiration, but it could also be called if
                            // the user manually stops the share, or if the app is destroyed.
                            final Handler handler = new Handler();
                            handler.postDelayed(stopTask, durationSec * 1000L);

                            // Now that sharing is active, we will turn the start button into a stop
                            // button with a countdown.
                            shareCountdown = new Timer();
                            shareCountdown.scheduleAtFixedRate(new TimerTask() {
                                private int counter = durationSec;

                                @Override
                                public void run() {
                                    if (counter >= 0) {
                                        runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                btnShare.setText(String.format(getString(R.string.btn_stop), secondsToTime(counter)));
                                            }
                                        });
                                    }
                                    counter -= 1;
                                }
                            }, 0L, 1000L);

                            // Re-enable the start (stop) button and inform the user.
                            btnShare.setEnabled(true);
                            labelStatusCur.setText(getString(R.string.label_status_wait));
                            labelStatusCur.setTextColor(getColor(R.color.statusWait));
                            diagSvc.showDialog(R.string.ok_title, R.string.ok_message);
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
     * On-tap handler for the "what's this" link underneath the checkbox for allowing adoption.
     * Opens an explanation of adoption.
     */
    public void explainAdoption(View view) {
        diagSvc.showDialog(R.string.explain_adopt_title, R.string.explain_adopt_body);
    }

    /**
     * On-tap handler for the "add an existing share" button. Opens a dialog for adding an existing
     * share to the group.
     */
    public void adoptShare(View view) {
        String server = txtServer.getText().toString();
        if (!server.endsWith("/")) server = server + "/";
        diagSvc.showDialog(R.string.adopt_title, String.format(getString(R.string.adopt_body), server), adoptBuilder);
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
                    resetTask.run();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                }
            }, resetTask);
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
        txtNickname = findViewById(R.id.txtNickname);
        txtPIN = findViewById(R.id.txtPIN);
        selMode = findViewById(R.id.selMode);
        btnShare = findViewById(R.id.btnShare);
        btnLink = findViewById(R.id.btnLink);
        labelStatusCur = findViewById(R.id.labelStatusCur);
        chkRemember = findViewById(R.id.chkRemember);
        labelAdoptWhatsThis = findViewById(R.id.labelAdoptWhatsThis);
        chkAllowAdopt = findViewById(R.id.chkAllowAdopt);
        rowAllowAdopt = findViewById(R.id.rowAllowAdopt);
        rowNickname = findViewById(R.id.rowNickname);
        rowPIN = findViewById(R.id.rowPIN);

        layoutGroupPIN = findViewById(R.id.layoutGroupPIN);
        labelShowPin = findViewById(R.id.labelShowPin);

        resetTask = new Runnable() {

            /**
             * A function which resets the user interface to its default settings, as if the app was
             * just opened. Used to reset the UI after errors and after sharing has expired.
             */
            @Override
            public void run() {
                if (shareCountdown != null) {
                    shareCountdown.cancel();
                    shareCountdown.purge();
                }

                labelStatusCur.setText(getString(R.string.label_status_none));
                labelStatusCur.setTextColor(getColor(R.color.statusOff));

                btnShare.setEnabled(true);
                btnShare.setText(R.string.btn_start);
                btnLink.setEnabled(false);

                txtServer.setEnabled(true);
                txtPassword.setEnabled(true);
                txtDuration.setEnabled(true);
                txtInterval.setEnabled(true);

                selMode.setEnabled(true);
                txtNickname.setEnabled(true);
                txtPIN.setEnabled(true);
                chkAllowAdopt.setEnabled(true);

                layoutGroupPIN.setVisibility(View.GONE);
            }
        };

        diagSvc = new DialogService(this);
        stopTask = new StopSharingTask(this, diagSvc, resetTask);
        shareCountdown = null;
    }

    private void loadPreferences() {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("connectionPrefs", MODE_PRIVATE);
        txtServer.setText(settings.getString("server", ""));
        txtDuration.setText(String.valueOf(settings.getInt("duration", 30)));
        txtInterval.setText(String.valueOf(settings.getInt("interval", 1)));
        txtPassword.setText(settings.getString("password", ""));
        txtNickname.setText(settings.getString("nickname", ""));
        chkRemember.setChecked(settings.getBoolean("rememberPassword", false));
        chkAllowAdopt.setChecked(settings.getBoolean("allowAdoption", true));
    }

    private void setPreferences(String server, int duration, int interval, String nickname, boolean allowAdoption) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("connectionPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("server", server);
        editor.putInt("duration", duration);
        editor.putInt("interval", interval);
        editor.putString("nickname", nickname);
        editor.putBoolean("allowAdoption", allowAdoption);
        editor.apply();
    }

    private void setPassword(boolean store, String password) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("connectionPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("rememberPassword", store);
        editor.putString("password", password);
        editor.apply();
    }

    private String secondsToTime(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h + ":");
        if (h > 0 && m < 10) sb.append("0");
        sb.append(m + ":");
        if (s < 10) sb.append("0");
        sb.append(s);

        return sb.toString();
    }
}
