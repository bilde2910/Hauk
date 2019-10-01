package info.varden.hauk.ui;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import info.varden.hauk.HaukConst;
import info.varden.hauk.R;
import info.varden.hauk.StopSharingTask;
import info.varden.hauk.dialog.AdoptDialogBuilder;
import info.varden.hauk.dialog.CustomDialogBuilder;
import info.varden.hauk.dialog.DialogButtons;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.http.NewLinkPacket;
import info.varden.hauk.http.Packet;
import info.varden.hauk.http.SessionInitiationPacket;
import info.varden.hauk.http.StopSharingPacket;
import info.varden.hauk.service.GNSSActiveHandler;
import info.varden.hauk.service.LocationPushService;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.struct.Version;
import info.varden.hauk.utils.ReceiverDataRegistry;
import info.varden.hauk.utils.ResumableSessions;
import info.varden.hauk.utils.TimeUtils;

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
    private EditText txtGroupCode;
    private Spinner selUnit;
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
    private TableLayout tableLinks;

    private LinearLayout layoutGroupPIN;
    private TextView labelShowPin;
    private Button btnAdopt;

    // A helper utility class for displaying dialog windows/message boxes.
    private DialogService dialogSvc;

    // A helper utility to resume sessions.
    private ResumableSessions resumable;

    // A runnable task that is executed when location sharing stops. It clears the persistent Hauk
    // notification, unregisters the location pusher and resets the UI to a fresh state.
    private StopSharingTask stopTask;

    // A timer that counts down the number of seconds left of the share period.
    private Timer shareCountdown;

    // A handler that cancels the share after expiry
    private Handler handler;

    // A runnable task that resets the UI to a fresh state.
    private Runnable resetTask;

    // A list of links displayed on the UI that the client is contributing to, paired with the View
    // representing the link of that share and its controls in the link list.
    private Map<String, View> shareList;

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setClassVariables();

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

        // Add an event handler to the sharing mode selector.
        selMode.setOnItemSelectedListener(new SelectionModeChangedListener());

        loadPreferences();
        this.resumable.tryResumeShare(new ResumableSessions.ResumeHandler() {

            @Override
            public void onSharesFetched(Context ctx, final Session session, final List<Share> shares) {
                // Prompt the user to continue the session.
                dialogSvc.showDialog(R.string.resume_title, String.format(ctx.getString(R.string.resume_body), shares.size(), session.getExpiryString()), DialogButtons.YES_NO, new CustomDialogBuilder() {

                    @Override
                    public void onPositive() {
                        // If yes, do continue the session.
                        if (session.isActive()) {
                            for (Share share : shares) {
                                share.setSession(session);
                                shareLocation(share);
                            }
                        }
                    }

                    @Override
                    public void onNegative() {
                        // If not, clear the resumption data so that the user isn't asked again for
                        // the share in question.
                        resumable.clearResumableSession();
                    }

                    @Override
                    public View createView(Context ctx) {
                        return null;
                    }
                });
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
    public void startSharing(@SuppressWarnings("unused") View view) {
        // If there is an executable stop task, that means that sharing is already active. Shut down
        // the share by running the stop task instead of starting a new share.
        if (stopTask.canExecute()) {
            stopTask.run();
            return;
        }

        // Disable the UI while we attempt to connect to the Hauk backend.
        disableUI();

        String server = txtServer.getText().toString().trim();
        final String password = txtPassword.getText().toString();
        int duration = Integer.parseInt(txtDuration.getText().toString());
        final int interval = Integer.parseInt(txtInterval.getText().toString());
        final String nickname = txtNickname.getText().toString().trim();
        final ShareMode mode = ShareMode.fromMode(selMode.getSelectedItemPosition());
        final String groupPin = txtGroupCode.getText().toString();
        final boolean allowAdoption = chkAllowAdopt.isChecked();
        final int durUnit = selUnit.getSelectedItemPosition();

        // Save connection preferences for next launch, so the user doesn't have to enter URL etc.
        // every time.
        setPreferences(server, duration, interval, durUnit, nickname, allowAdoption);

        // If password saving is enabled, save the password as well.
        if (chkRemember.isChecked()) setPassword(true, password);

        assert mode != null;

        // Create a "full" server address, with a following slash if it is missing. This is used to
        // construct sub-paths for the Hauk backend.
        final String serverFull = server.endsWith("/") ? server : server + "/";

        // The backend takes duration in seconds, so convert the minutes supplied by the user.
        switch (durUnit) {
            case HaukConst.DURATION_UNIT_MINUTES:
                duration *= TimeUtils.SECONDS_PER_MINUTE;
                break;
            case HaukConst.DURATION_UNIT_HOURS:
                duration *= TimeUtils.SECONDS_PER_HOUR;
                break;
            case HaukConst.DURATION_UNIT_DAYS:
                duration *= TimeUtils.SECONDS_PER_DAY;
                break;
        }
        final int durationSec = duration;

        // Check for location permission and prompt the user if missing. This returns because the
        // checking function creates async dialogs here - the user is prompted to press the button
        // again instead.
        if (!hasLocationPermission()) return;

        final LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled;
        try {
            assert locMan != null;
            isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
            isGPSEnabled = false;
        }
        if (!isGPSEnabled) {
            dialogSvc.showDialog(R.string.err_client, R.string.err_location_disabled, resetTask);
            return;
        }

        // Create a progress dialog while doing initial handshake. This could end up taking a while
        // (e.g. if the host is unreachable, it will eventually time out), and having a progress bar
        // makes for better UX since it visually shows that something is actually happening in the
        // background.
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setTitle(R.string.progress_connect_title);
        progress.setMessage(getString(R.string.progress_connect_body));
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();

        // Create a handler for our request to initiate a new session. This is declared separately
        // from the SessionInitiationPackets below to avoid code duplication.
        final SessionInitiationPacket.ResponseHandler handler = new SessionInitiationPacket.ResponseHandler() {
            @Override
            public void onSessionInitiated(Share share) {
                progress.dismiss();

                // Proceed with the location share.
                resumable.setSessionResumable(share.getSession());
                resumable.addShareResumable(share);
                shareLocation(share);
            }

            @Override
            public void onFailure(Exception ex) {
                progress.dismiss();
                if (ex instanceof MalformedURLException) {
                    ex.printStackTrace();
                    dialogSvc.showDialog(R.string.err_client, R.string.err_malformed_url, resetTask);
                } else if (ex instanceof IOException) {
                    ex.printStackTrace();
                    dialogSvc.showDialog(R.string.err_connect, ex.getMessage(), resetTask);
                } else {
                    ex.printStackTrace();
                    dialogSvc.showDialog(R.string.err_server, ex.getMessage(), resetTask);
                }
            }

            @Override
            public void onShareModeIncompatible(Version backendVersion) {
                selMode.setSelection(ShareMode.CREATE_ALONE.getMode());
                dialogSvc.showDialog(R.string.err_outdated, String.format(getString(R.string.err_ver_group), HaukConst.VERSION_COMPAT_GROUP_SHARE, backendVersion));
            }
        };

        // Create a handshake request and handle the response. The handshake transmits the duration
        // and interval to the server and waits for the server to return a session ID to confirm
        // session creation.
        Packet pkt = null;
        switch (mode) {
            case CREATE_ALONE:
                pkt = new SessionInitiationPacket(this, serverFull, password, durationSec, interval, allowAdoption) {
                    @Override
                    public ResponseHandler getHandler() {
                        return handler;
                    }
                };
                break;

            case CREATE_GROUP:
                pkt = new SessionInitiationPacket(this, serverFull, password, durationSec, interval, nickname) {
                    @Override
                    public ResponseHandler getHandler() {
                        return handler;
                    }
                };
                break;

            case JOIN_GROUP:
                pkt = new SessionInitiationPacket(this, serverFull, password, durationSec, interval, nickname, groupPin) {
                    @Override
                    public ResponseHandler getHandler() {
                        return handler;
                    }
                };
                break;
        }

        pkt.send();
    }

    /**
     * Disables all UI elements that should be read-only while sharing.
     */
    private void disableUI() {
        btnShare.setEnabled(false);
        txtServer.setEnabled(false);
        txtPassword.setEnabled(false);
        txtDuration.setEnabled(false);
        txtInterval.setEnabled(false);

        selUnit.setEnabled(false);
        selMode.setEnabled(false);
        txtNickname.setEnabled(false);
        txtGroupCode.setEnabled(false);
        chkAllowAdopt.setEnabled(false);
    }

    /**
     * Executes a location sharing session against the server. This can be a new session, or a
     * resumed session.
     *
     * @param share The share to run against the server.
     */
    private void shareLocation(final Share share) {
        // Disable the UI if it's not already disabled.
        disableUI();

        if (share.getShareMode() == ShareMode.CREATE_GROUP) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Show the group PIN on the UI if a new group share was created.
                    labelShowPin.setText(share.getJoinCode());
                    btnAdopt.setOnClickListener(new InitiateAdoptionClickHandler(share));
                    layoutGroupPIN.setVisibility(View.VISIBLE);
                }
            });
        }

        // We now have a link to share, so we enable the additional link creation button. Add an
        // event handler to handle the user clicking on it.
        btnLink.setOnClickListener(new AddLinkClickListener(share.getSession()));
        btnLink.setEnabled(true);

        // Also make sure to add a link row to the table of active links.
        addLink(share);

        // Even though we previously requested location permission, we still have to
        // check for it when we actually use the location API (user could have
        // disabled it while connecting).
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Create a client that receives location updates and pushes these to
            // the Hauk backend.
            Intent pusher = new Intent(MainActivity.this, LocationPushService.class);
            pusher.setAction(LocationPushService.ACTION_ID);
            pusher.putExtra(HaukConst.EXTRA_SHARE, ReceiverDataRegistry.register(share));
            pusher.putExtra(HaukConst.EXTRA_STOP_TASK, ReceiverDataRegistry.register(stopTask));
            pusher.putExtra(HaukConst.EXTRA_GNSS_ACTIVE_TASK, ReceiverDataRegistry.register(new GNSSActiveHandler() {
                @Override
                public void onCoarseLocationReceived() {
                    // Indicate to the user that GPS data is being received when the
                    // location pusher starts receiving GPS data.
                    labelStatusCur.setText(getString(R.string.label_status_coarse));
                }

                @Override
                public void onAccurateLocationReceived() {
                    // Indicate to the user that GPS data is being received when the
                    // location pusher starts receiving GPS data.
                    labelStatusCur.setText(getString(R.string.label_status_ok));
                    labelStatusCur.setTextColor(getColor(R.color.statusOn));
                }

                @Override
                public void onShareListReceived(final String linkFormat, final String[] shareIDs) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            List<String> currentShares = Arrays.asList(shareIDs);
                            for (int i = 0; i < currentShares.size(); i++) {
                                String shareID = currentShares.get(i);
                                if (!shareList.containsKey(shareID)) {
                                    // A new share has been added. If the client is suddenly informed of a
                                    // new share, it is always a group share because that is the only type
                                    // of shares that can be initiated by a remote user (through adoption).
                                    Share newShare = new Share(share.getSession(), String.format(linkFormat, shareID), shareID, ShareMode.JOIN_GROUP);
                                    addLink(newShare);
                                    resumable.addShareResumable(newShare);
                                }
                            }
                            for (Iterator<Map.Entry<String, View>> it = shareList.entrySet().iterator(); it.hasNext();) {
                                Map.Entry<String, View> entry = it.next();
                                if (!currentShares.contains(entry.getKey())) {
                                    // A share has been removed.
                                    tableLinks.removeView(entry.getValue());
                                    it.remove();
                                    resumable.removeResumableShare(entry.getKey());
                                }
                            }
                        }
                    });
                }
            }));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            handler.postDelayed(stopTask, share.getSession().getRemainingMillis());

            // Now that sharing is active, we will turn the start button into a stop
            // button with a countdown.
            shareCountdown = new Timer();
            shareCountdown.scheduleAtFixedRate(new TimerTask() {
                private int counter = share.getSession().getRemainingSeconds();

                @Override
                public void run() {
                    if (counter >= 0) {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                btnShare.setText(String.format(getString(R.string.btn_stop), TimeUtils.secondsToTime(counter)));
                            }
                        });
                    }
                    counter -= 1;
                }
            }, 0L, TimeUtils.MILLIS_PER_SECOND);

            // Re-enable the start (stop) button and inform the user.
            btnShare.setEnabled(true);
            labelStatusCur.setText(getString(R.string.label_status_wait));
            labelStatusCur.setTextColor(getColor(R.color.statusWait));
            dialogSvc.showDialog(R.string.ok_title, R.string.ok_message);
        } else {
            dialogSvc.showDialog(R.string.err_client, R.string.err_missing_perms, resetTask);
        }
    }

    /**
     * Adds a new sharing link to the list of active links.
     *
     * @param share The share to add to the list of links.
     */
    private void addLink(Share share) {
        // Get the table row layout and inflate it into a view.
        LayoutInflater inflater = getLayoutInflater();
        View linkView = inflater.inflate(R.layout.content_link, null);

        // Add an event handler for the stop button. This will stop the given share only.
        Button btnStop = linkView.findViewById(R.id.linkBtnStop);
        if (share.getSession().getBackendVersion().atLeast(HaukConst.VERSION_COMPAT_VIEW_ID)) {
            btnStop.setOnClickListener(new StopLinkClickHandler(share));
        } else {
            btnStop.setVisibility(View.GONE);
        }

        // Add an event handler for the share button.
        Button btnShare = linkView.findViewById(R.id.linkBtnShare);
        btnShare.setOnClickListener(new ShareLinkClickHandler(share));

        // Update the text on the UI.
        TextView txtLink = linkView.findViewById(R.id.linkTxtLink);
        txtLink.setText(share.getID());
        TextView txtDesc = linkView.findViewById(R.id.linkTxtDesc);
        txtDesc.setText(getString(share.getShareMode().getDescriptorResource()));

        // Add the view to the list of entries, so it can be removed later if the user stops the
        // share.
        shareList.put(share.getID(), linkView);
        tableLinks.addView(linkView);
    }

    /**
     * On-tap handler for the "what's this" link underneath the checkbox for allowing adoption.
     * Opens an explanation of adoption.
     */
    public void explainAdoption(@SuppressWarnings("unused") View view) {
        dialogSvc.showDialog(R.string.explain_adopt_title, R.string.explain_adopt_body);
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
            dialogSvc.showDialog(R.string.req_perms_title, R.string.req_perms_message, new Runnable() {

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
        txtGroupCode = findViewById(R.id.txtGroupCode);
        selUnit = findViewById(R.id.selUnit);
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
        tableLinks = findViewById(R.id.tableLinks);

        layoutGroupPIN = findViewById(R.id.layoutGroupPIN);
        labelShowPin = findViewById(R.id.labelShowPin);
        btnAdopt = findViewById(R.id.btnAdopt);

        shareList = new HashMap<>();

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
                    shareCountdown = null;
                }

                labelStatusCur.setText(getString(R.string.label_status_none));
                labelStatusCur.setTextColor(getColor(R.color.statusOff));

                btnShare.setEnabled(true);
                btnShare.setText(R.string.btn_start);
                btnLink.setEnabled(false);
                btnLink.setOnClickListener(null);

                txtServer.setEnabled(true);
                txtPassword.setEnabled(true);
                txtDuration.setEnabled(true);
                txtInterval.setEnabled(true);

                selUnit.setEnabled(true);
                selMode.setEnabled(true);
                txtNickname.setEnabled(true);
                txtGroupCode.setEnabled(true);
                chkAllowAdopt.setEnabled(true);

                layoutGroupPIN.setVisibility(View.GONE);
                btnAdopt.setOnClickListener(null);

                tableLinks.removeAllViews();
                shareList.clear();

                resumable.clearResumableSession();
            }
        };

        dialogSvc = new DialogService(this);
        resumable = new ResumableSessions(this);
        handler = new Handler();
        stopTask = new StopSharingTask(this, dialogSvc, resetTask, handler);
        shareCountdown = null;
    }

    private void loadPreferences() {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(HaukConst.SHARED_PREFS_CONNECTION, MODE_PRIVATE);
        txtServer.setText(settings.getString(HaukConst.PREF_SERVER, HaukConst.DEFAULT_SERVER));
        txtDuration.setText(String.valueOf(settings.getInt(HaukConst.PREF_DURATION, HaukConst.DEFAULT_DURATION)));
        txtInterval.setText(String.valueOf(settings.getInt(HaukConst.PREF_INTERVAL, HaukConst.DEFAULT_INTERVAL)));
        txtPassword.setText(settings.getString(HaukConst.PREF_PASSWORD, HaukConst.DEFAULT_PASSWORD));
        txtNickname.setText(settings.getString(HaukConst.PREF_NICKNAME, HaukConst.DEFAULT_NICKNAME));
        selUnit.setSelection(settings.getInt(HaukConst.PREF_DURATION_UNIT, HaukConst.DEFAULT_DURATION_UNIT));
        chkRemember.setChecked(settings.getBoolean(HaukConst.PREF_REMEMBER_PASSWORD, HaukConst.DEFAULT_REMEMBER_PASSWORD));
        chkAllowAdopt.setChecked(settings.getBoolean(HaukConst.PREF_ALLOW_ADOPTION, HaukConst.DEFAULT_ALLOW_ADOPTION));
    }

    private void setPreferences(String server, int duration, int interval, int durUnit, String nickname, boolean allowAdoption) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(HaukConst.SHARED_PREFS_CONNECTION, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(HaukConst.PREF_SERVER, server);
        editor.putInt(HaukConst.PREF_DURATION, duration);
        editor.putInt(HaukConst.PREF_INTERVAL, interval);
        editor.putString(HaukConst.PREF_NICKNAME, nickname);
        editor.putInt(HaukConst.PREF_DURATION_UNIT, durUnit);
        editor.putBoolean(HaukConst.PREF_ALLOW_ADOPTION, allowAdoption);
        editor.apply();
    }

    private void setPassword(boolean store, String password) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(HaukConst.SHARED_PREFS_CONNECTION, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean(HaukConst.PREF_REMEMBER_PASSWORD, store);
        editor.putString(HaukConst.PREF_PASSWORD, password);
        editor.apply();
    }

    private class StopLinkClickHandler implements View.OnClickListener {
        private final Share share;

        private StopLinkClickHandler(Share share) {
            this.share = share;
        }

        @Override
        public void onClick(View view) {
            new StopSharingPacket(MainActivity.this, this.share) {
                // TODO: Do something meaningful here?

                @Override
                public void onSuccess() {
                }

                @Override
                protected void onFailure(Exception ex) {
                    ex.printStackTrace();
                }
            }.send();
        }
    }

    private class ShareLinkClickHandler implements View.OnClickListener {
        private final Share share;

        private ShareLinkClickHandler(Share share) {
            this.share = share;
        }

        @Override
        public void onClick(View view) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            //noinspection HardCodedStringLiteral
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject));
            shareIntent.putExtra(Intent.EXTRA_TEXT, this.share.getViewURL());
            startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_via)));
        }
    }

    private class InitiateAdoptionClickHandler implements View.OnClickListener {
        private final Share share;

        private InitiateAdoptionClickHandler(Share share) {
            this.share = share;
        }

        @Override
        public void onClick(View view) {
            dialogSvc.showDialog(R.string.adopt_title, String.format(getString(R.string.adopt_body), this.share.getSession().getServerURL()), DialogButtons.OK_CANCEL, new AdoptDialogBuilder(MainActivity.this, this.share) {
                @Override
                public void onSuccess(final String nick) {
                    dialogSvc.showDialog(R.string.adopted_title, String.format(getString(R.string.adopted_body), nick));
                }

                @Override
                public void onFailure(final Exception ex) {
                    dialogSvc.showDialog(R.string.err_server, ex.getMessage());
                }
            });
        }
    }

    private class AddLinkClickListener implements View.OnClickListener {
        private final Session session;

        private AddLinkClickListener(Session session) {
            this.session = session;
        }

        @Override
        public void onClick(View view) {
            dialogSvc.showDialog(R.string.create_link_title, R.string.create_link_body, DialogButtons.CREATE_CANCEL, new CustomDialogBuilder() {

                private CheckBox chkAdopt;

                @Override
                public void onPositive() {
                    // Create a progress dialog while creating the link. This could end up
                    // taking a while (e.g. if the host is unreachable, it will eventually time
                    // out), and having a progress bar makes for better UX since it visually
                    // shows that something is actually happening in the background.
                    final ProgressDialog progress = new ProgressDialog(MainActivity.this);
                    progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progress.setTitle(R.string.progress_new_link_title);
                    progress.setMessage(getString(R.string.progress_new_link_body));
                    progress.setIndeterminate(true);
                    progress.setCancelable(false);
                    progress.show();

                    new NewLinkPacket(MainActivity.this, session, chkAdopt.isChecked()) {

                        @Override
                        public void onShareCreated(final Share share) {
                            progress.dismiss();
                            resumable.addShareResumable(share);

                            // Add the link to the list of active links.
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addLink(share);
                                }
                            });

                            // Tell the user that the link was added successfully.
                            dialogSvc.showDialog(R.string.link_added_title, R.string.link_added_body);
                        }

                        @Override
                        protected void onFailure(Exception ex) {
                            progress.dismiss();
                            if (ex instanceof MalformedURLException) {
                                ex.printStackTrace();
                                dialogSvc.showDialog(R.string.err_client, R.string.err_malformed_url);
                            } else if (ex instanceof IOException) {
                                ex.printStackTrace();
                                dialogSvc.showDialog(R.string.err_connect, ex.getMessage());
                            } else {
                                ex.printStackTrace();
                                dialogSvc.showDialog(R.string.err_server, ex.getMessage());
                            }
                        }
                    }.send();
                }

                @Override
                public void onNegative() {
                }

                @Override
                public View createView(Context ctx) {
                    // Create a dialog that prompts the user for the new share's adoption state.
                    LayoutInflater inflater = getLayoutInflater();
                    @SuppressWarnings("HardCodedStringLiteral")
                    @SuppressLint("InflateParams")
                    View dialogView = inflater.inflate(R.layout.dialog_create_link, null);

                    // Inherit the adoption state from the main share/saved preference.
                    this.chkAdopt = dialogView.findViewById(R.id.dialogNewLinkChkAdopt);
                    this.chkAdopt.setChecked(chkAllowAdopt.isChecked());

                    return dialogView;
                }
            });
        }
    }

    private class SelectionModeChangedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int selection, long rowId) {
            // This handler determines which UI elements should be visible, based on the user's
            // selection of sharing modes.
            switch (Objects.requireNonNull(ShareMode.fromMode(selection))) {
                case CREATE_ALONE:
                    rowAllowAdopt.setVisibility(View.VISIBLE);
                    rowNickname.setVisibility(View.GONE);
                    rowPIN.setVisibility(View.GONE);
                    break;
                case CREATE_GROUP:
                    rowAllowAdopt.setVisibility(View.GONE);
                    rowNickname.setVisibility(View.VISIBLE);
                    rowPIN.setVisibility(View.GONE);
                    break;
                case JOIN_GROUP:
                    rowAllowAdopt.setVisibility(View.GONE);
                    rowNickname.setVisibility(View.VISIBLE);
                    rowPIN.setVisibility(View.VISIBLE);
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }
}
