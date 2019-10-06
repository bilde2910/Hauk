package info.varden.hauk.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.caching.ResumePrompt;
import info.varden.hauk.dialog.Buttons;
import info.varden.hauk.dialog.CustomDialogBuilder;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.http.SessionInitiationPacket;
import info.varden.hauk.manager.PromptCallback;
import info.varden.hauk.manager.SessionInitiationResponseHandler;
import info.varden.hauk.manager.SessionListener;
import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.manager.ShareListener;
import info.varden.hauk.struct.AdoptabilityPreference;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.struct.Version;
import info.varden.hauk.system.LocationPermissionsNotGrantedException;
import info.varden.hauk.system.LocationServicesDisabledException;
import info.varden.hauk.ui.listener.AddLinkClickListener;
import info.varden.hauk.ui.listener.InitiateAdoptionClickListener;
import info.varden.hauk.ui.listener.RememberPasswordPreferenceChangedListener;
import info.varden.hauk.ui.listener.SelectionModeChangedListener;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.PreferenceManager;
import info.varden.hauk.utils.TimeUtils;

/**
 * The main activity for Hauk.
 *
 * @author Marius Lindvall
 */
public final class MainActivity extends AppCompatActivity {

    /**
     * A list of UI components that should be set uneditable for as long as a session is running.
     */
    private View[] lockWhileRunning;

    /**
     * A helper utility class for displaying dialog windows/message boxes.
     */
    private DialogService dialogSvc;

    /**
     * A timer that counts down the number of seconds left of the share period.
     */
    private TextViewCountdownRunner shareCountdown;

    /**
     * A runnable task that resets the UI to a fresh state.
     */
    private Runnable uiResetTask;

    /**
     * A callback that is run when sharing is stopped to reset the UI to a fresh state.
     */
    private StopSharingUICallback uiStopTask;

    /**
     * An instance that manages all sessions and shares.
     */
    private SessionManager manager;

    /**
     * A manager that adds and removes share links to and from a UI component dedicated to that
     * purpose when a share is joined or left.
     */
    private ShareLinkLayoutManager linkList;

    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("Creating main activity"); //NON-NLS
        setContentView(R.layout.activity_main);
        setClassVariables();
        ((TextView) findViewById(R.id.labelAdoptWhatsThis)).setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        Log.d("Attaching event handlers"); //NON-NLS

        // Add an on checked handler to the password remember checkbox to save their password
        // immediately.
        ((CompoundButton) findViewById(R.id.chkRemember)).setOnCheckedChangeListener(
                new RememberPasswordPreferenceChangedListener(this, (EditText) findViewById(R.id.txtPassword))
        );

        // Add an event handler to the sharing mode selector.
        //noinspection OverlyStrongTypeCast
        ((Spinner) findViewById(R.id.selMode)).setOnItemSelectedListener(new SelectionModeChangedListener(
                findViewById(R.id.rowAllowAdopt),
                findViewById(R.id.rowNickname),
                findViewById(R.id.rowPIN)
        ));

        loadPreferences();
        this.manager.resumeShares(new ResumePrompt() {
            @Override
            public void promptForResumption(Context ctx, Session session, Share[] shares, PromptCallback response) {
                // Prompt the user to continue the session.
                new DialogService(ctx).showDialog(
                        R.string.resume_title,
                        String.format(ctx.getString(R.string.resume_body), shares.length, session.getExpiryString()),
                        Buttons.YES_NO,
                        new ResumeDialogBuilder(response)
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        this.uiStopTask.setActivityDestroyed();
        super.onDestroy();
    }

    /**
     * On-tap handler for the "start sharing" and "stop sharing" button.
     */
    public void startSharing(@SuppressWarnings("unused") View view) {
        // If there is an executable stop task, that means that sharing is already active. Shut down
        // the share by running the stop task instead of starting a new share.
        if (this.manager.isSessionActive()) {
            Log.i("Sharing is being stopped from main activity"); //NON-NLS
            this.manager.stopSharing();
            return;
        }

        // Disable the UI while we attempt to connect to the Hauk backend.
        findViewById(R.id.btnShare).setEnabled(false);
        disableUI();

        String server = ((TextView) findViewById(R.id.txtServer)).getText().toString().trim();
        String password = ((TextView) findViewById(R.id.txtPassword)).getText().toString();
        int duration = Integer.parseInt(((TextView) findViewById(R.id.txtDuration)).getText().toString());
        int interval = Integer.parseInt(((TextView) findViewById(R.id.txtInterval)).getText().toString());
        String nickname = ((TextView) findViewById(R.id.txtNickname)).getText().toString().trim();
        @SuppressWarnings("OverlyStrongTypeCast") ShareMode mode = ShareMode.fromMode(((Spinner) findViewById(R.id.selMode)).getSelectedItemPosition());
        String groupPin = ((TextView) findViewById(R.id.txtGroupCode)).getText().toString();
        boolean allowAdoption = ((Checkable) findViewById(R.id.chkAllowAdopt)).isChecked();
        @SuppressWarnings("OverlyStrongTypeCast") int durUnit = ((Spinner) findViewById(R.id.selUnit)).getSelectedItemPosition();

        // Save connection preferences for next launch, so the user doesn't have to enter URL etc.
        // every time.
        Log.i("Updating connection preferences"); //NON-NLS
        PreferenceManager prefs = new PreferenceManager(this);
        prefs.set(Constants.PREF_SERVER, server);
        prefs.set(Constants.PREF_DURATION, duration);
        prefs.set(Constants.PREF_INTERVAL, interval);
        prefs.set(Constants.PREF_DURATION_UNIT, durUnit);
        prefs.set(Constants.PREF_NICKNAME, nickname);
        prefs.set(Constants.PREF_ALLOW_ADOPTION, allowAdoption);

        // If password saving is enabled, save the password as well.
        if (((Checkable) findViewById(R.id.chkRemember)).isChecked()) {
            Log.i("Saving password"); //NON-NLS
            prefs.set(Constants.PREF_REMEMBER_PASSWORD, true);
            prefs.set(Constants.PREF_PASSWORD, password);
        }

        assert mode != null;
        server = server.endsWith("/") ? server : server + "/";

        // The backend takes duration in seconds, so convert the minutes supplied by the user.
        duration = TimeUtils.timeUnitsToSeconds(duration, durUnit);

        SessionInitiationPacket.InitParameters initParams = new SessionInitiationPacket.InitParameters(server, password, duration, interval);
        SessionInitiationResponseHandler responseHandler = new SessionInitiationResponseHandlerImpl();

        try {
            switch (mode) {
                case CREATE_ALONE:
                    this.manager.shareLocation(initParams, responseHandler, allowAdoption ? AdoptabilityPreference.ALLOW_ADOPTION : AdoptabilityPreference.DISALLOW_ADOPTION);
                    break;

                case CREATE_GROUP:
                    this.manager.shareLocation(initParams, responseHandler, nickname);
                    break;

                case JOIN_GROUP:
                    this.manager.shareLocation(initParams, responseHandler, nickname, groupPin);
                    break;

                default:
                    Log.wtf("Unknown sharing mode. This is not supposed to happen, ever"); //NON-NLS
                    break;
            }
        } catch (LocationServicesDisabledException e) {
            Log.e("Share initiation was stopped because location services are disabled", e); //NON-NLS
            this.dialogSvc.showDialog(R.string.err_client, R.string.err_location_disabled, this.uiResetTask);
        } catch (LocationPermissionsNotGrantedException e) {
            Log.w("Share initiation was stopped because the user has not granted location permissions yet", e); //NON-NLS
        }
    }

    /**
     * Disables all UI elements that should be read-only while sharing.
     */
    private void disableUI() {
        Log.i("Disabling user interface"); //NON-NLS
        for (View view : this.lockWhileRunning) {
            view.setEnabled(false);
        }
    }

    /**
     * On-tap handler for the "what's this" link underneath the checkbox for allowing adoption.
     * Opens an explanation of adoption.
     */
    public void explainAdoption(@SuppressWarnings("unused") View view) {
        Log.i("Explaining share adoption upon user request"); //NON-NLS
        this.dialogSvc.showDialog(R.string.explain_adopt_title, R.string.explain_adopt_body);
    }

    /**
     * This function is called by onCreate() to initialize class-level variables for usage in this
     * activity.
     */
    private void setClassVariables() {
        Log.d("Setting class variables"); //NON-NLS
        this.lockWhileRunning = new View[] {
                findViewById(R.id.txtServer),
                findViewById(R.id.txtPassword),
                findViewById(R.id.txtDuration),
                findViewById(R.id.txtInterval),

                findViewById(R.id.selUnit),
                findViewById(R.id.selMode),
                findViewById(R.id.txtNickname),
                findViewById(R.id.txtGroupCode),
                findViewById(R.id.chkAllowAdopt)
        };

        this.uiResetTask = new ResetTask();
        this.uiStopTask = new StopSharingUICallback(this, this.uiResetTask);
        this.shareCountdown = new TextViewCountdownRunner((TextView) findViewById(R.id.btnShare), getString(R.string.btn_stop));
        this.dialogSvc = new DialogService(this);

        this.manager = new SessionManager(this, this.uiStopTask) {
            @Override
            protected void requestLocationPermission() {
                // Show a rationale first before requesting location permission, giving users the chance
                // to cancel the request if they so desire. Users are informed that they must click the
                // "start sharing" button again after they have granted the permission.
                MainActivity.this.dialogSvc.showDialog(R.string.req_perms_title, R.string.req_perms_message, new PermissionRequestExecutionTask(), MainActivity.this.uiResetTask);
            }
        };

        this.manager.attachStatusListener(new GNSSStatusLabelUpdater(this, (TextView) findViewById(R.id.labelStatusCur)));
        this.manager.attachShareListener(new ShareListenerImpl());
        this.manager.attachSessionListener(new SessionListenerImpl());

        this.linkList = new ShareLinkLayoutManager(this, this.manager, (ViewGroup) findViewById(R.id.tableLinks));
    }

    /**
     * Loads preferences from storage and applies them to the UI.
     */
    private void loadPreferences() {
        Log.i("Loading preferences..."); //NON-NLS
        PreferenceManager prefs = new PreferenceManager(this);
        ((TextView) findViewById(R.id.txtServer)).setText(prefs.get(Constants.PREF_SERVER));
        ((TextView) findViewById(R.id.txtDuration)).setText(String.valueOf(prefs.get(Constants.PREF_DURATION)));
        ((TextView) findViewById(R.id.txtInterval)).setText(String.valueOf(prefs.get(Constants.PREF_INTERVAL)));
        ((TextView) findViewById(R.id.txtPassword)).setText(prefs.get(Constants.PREF_PASSWORD));
        ((TextView) findViewById(R.id.txtNickname)).setText(prefs.get(Constants.PREF_NICKNAME));
        // Because I can choose between an unchecked cast warning and an overly strong cast warning,
        // I'm going to with the latter.
        //noinspection OverlyStrongTypeCast
        ((Spinner) findViewById(R.id.selUnit)).setSelection(prefs.get(Constants.PREF_DURATION_UNIT));
        ((Checkable) findViewById(R.id.chkRemember)).setChecked(prefs.get(Constants.PREF_REMEMBER_PASSWORD));
        ((Checkable) findViewById(R.id.chkAllowAdopt)).setChecked(prefs.get(Constants.PREF_ALLOW_ADOPTION));
    }

    /**
     * An implementation of {@link SessionManager}'s session initiation response handler that spawns
     * a progress dialog and shows alerts if applicable.
     */
    private final class SessionInitiationResponseHandlerImpl extends DialogPacketFailureHandler implements SessionInitiationResponseHandler {
        private ProgressDialog progress;

        private SessionInitiationResponseHandlerImpl() {
            super(new DialogService(MainActivity.this), MainActivity.this.uiResetTask);
        }

        @Override
        public void onInitiating() {
            // Create a progress dialog while doing initial handshake. This could end up taking a
            // while (e.g. if the host is unreachable, it will eventually time out), and having a
            // progress bar makes for better UX since it visually shows that something is actually
            // happening in the background.
            this.progress = new ProgressDialog(MainActivity.this);
            this.progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            this.progress.setTitle(R.string.progress_connect_title);
            this.progress.setMessage(getString(R.string.progress_connect_body));
            this.progress.setIndeterminate(true);
            this.progress.setCancelable(false);
            this.progress.show();
        }

        @Override
        public void onSuccess() {
            this.progress.dismiss();
        }

        @Override
        protected void onBeforeShowFailureDialog() {
            this.progress.dismiss();
        }

        @Override
        public void onShareModeForciblyDowngraded(ShareMode downgradeTo, Version backendVersion) {
            //noinspection OverlyStrongTypeCast
            ((Spinner) findViewById(R.id.selMode)).setSelection(downgradeTo.getIndex());
            MainActivity.this.dialogSvc.showDialog(R.string.err_outdated, String.format(getString(R.string.err_ver_group), Constants.VERSION_COMPAT_GROUP_SHARE, backendVersion));
        }
    }

    /**
     * A function which resets the user interface to its default settings, as if the app was just
     * opened. Used to reset the UI after errors and after sharing has expired.
     */
    private final class ResetTask implements Runnable {
        @Override
        public void run() {
            Log.i("Reset task called; resetting UI..."); //NON-NLS
            MainActivity.this.shareCountdown.stop();

            Button btnShare = findViewById(R.id.btnShare);
            btnShare.setEnabled(true);
            btnShare.setText(R.string.btn_start);

            Button btnLink = findViewById(R.id.btnLink);
            btnLink.setEnabled(false);
            btnLink.setOnClickListener(null);

            for (View v : MainActivity.this.lockWhileRunning) {
                v.setEnabled(true);
            }

            findViewById(R.id.layoutGroupPIN).setVisibility(View.GONE);
            findViewById(R.id.btnAdopt).setOnClickListener(null);

            MainActivity.this.linkList.removeAll();
            Log.i("App state was reset"); //NON-NLS
        }
    }

    /**
     * A dialog builder that hooks into {@link PromptCallback} for accepting or denying session
     * resumption.
     */
    private final class ResumeDialogBuilder implements CustomDialogBuilder {
        private final PromptCallback response;

        private ResumeDialogBuilder(PromptCallback response) {
            this.response = response;
        }

        @Override
        public void onPositive() {
            disableUI();
            this.response.accept();
        }

        @Override
        public void onNegative() {
            this.response.deny();
        }

        @Nullable
        @Override
        public View createView(Context ctx) {
            return null;
        }
    }

    /**
     * Function that runs if the user accepts the location request rationale via the OK button.
     */
    private final class PermissionRequestExecutionTask implements Runnable {
        @Override
        public void run() {
            Log.i("User accepted location permission rationale; showing permission request from system"); //NON-NLS
            MainActivity.this.uiResetTask.run();
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    /**
     * Implementation of {@link SessionListener} that updates the UI and shows dialog depending on
     * the results of session creation.
     */
    private final class SessionListenerImpl implements SessionListener {
        @Override
        public void onSessionCreated(Session session) {
            // We now have a link to share, so we enable the additional link creation button if the backend supports it. Add an event handler to handle the user clicking on it.
            if (session.getBackendVersion().isAtLeast(Constants.VERSION_COMPAT_VIEW_ID)) {
                boolean allowNewLinkAdoption = ((Checkable) findViewById(R.id.chkAllowAdopt)).isChecked();
                Button btnLink = findViewById(R.id.btnLink);
                Log.d("Adding event handler for add-link button"); //NON-NLS
                btnLink.setOnClickListener(new AddLinkClickListener(MainActivity.this, session, allowNewLinkAdoption) {
                    @Override
                    public void onShareCreated(Share share) {
                        MainActivity.this.manager.shareLocation(share);
                    }
                });
                btnLink.setEnabled(true);
            } else {
                Log.w("Backend is outdated and does not support adding additional links. Button will remain disabled."); //NON-NLS
            }

            // Now that sharing is active, we will turn the start button into a stop
            // button with a countdown.
            Log.i("Scheduling countdown to update every second"); //NON-NLS
            MainActivity.this.shareCountdown.start(session.getRemainingSeconds());

            // Re-enable the start (stop) button and inform the user.
            findViewById(R.id.btnShare).setEnabled(true);

            MainActivity.this.dialogSvc.showDialog(R.string.ok_title, R.string.ok_message);
        }

        @Override
        public void onSessionCreationFailedDueToPermissions() {
            MainActivity.this.dialogSvc.showDialog(R.string.err_client, R.string.err_missing_perms, MainActivity.this.uiResetTask);
        }
    }

    /**
     * Implementation of {@link ShareListener} that adds and removes shares from the UI when shares
     * are joined into or left.
     */
    private final class ShareListenerImpl implements ShareListener {
        @Override
        public void onShareJoined(Share share) {
            MainActivity.this.linkList.add(share);
            if (share.getShareMode() == ShareMode.CREATE_GROUP) {
                runOnUiThread(new ShowGroupPINLayoutTask(share));
            }
        }

        @Override
        public void onShareParted(Share share) {
            MainActivity.this.linkList.remove(share);
        }
    }

    /**
     * A function that shows the group PIN layout and binds the adoption button click listener when
     * a group share is created.
     */
    private final class ShowGroupPINLayoutTask implements Runnable {
        private final Share share;

        private ShowGroupPINLayoutTask(Share share) {
            this.share = share;
        }

        @Override
        public void run() {
            // Show the group PIN on the UI if a new group share was created.
            Log.d("Showing group layout"); //NON-NLS
            ((TextView) findViewById(R.id.labelShowPin)).setText(this.share.getJoinCode());
            findViewById(R.id.btnAdopt).setOnClickListener(new InitiateAdoptionClickListener(MainActivity.this, this.share));
            findViewById(R.id.layoutGroupPIN).setVisibility(View.VISIBLE);
        }
    }
}
