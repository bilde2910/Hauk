package info.varden.hauk.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.caching.ResumePrompt;
import info.varden.hauk.dialog.Buttons;
import info.varden.hauk.dialog.CustomDialogBuilder;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.dialog.StopSharingConfirmationPrompt;
import info.varden.hauk.http.SessionInitiationPacket;
import info.varden.hauk.manager.PromptCallback;
import info.varden.hauk.manager.SessionInitiationReason;
import info.varden.hauk.manager.SessionInitiationResponseHandler;
import info.varden.hauk.manager.SessionListener;
import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.manager.ShareListener;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.struct.Version;
import info.varden.hauk.system.launcher.OpenLinkListener;
import info.varden.hauk.system.powersaving.DeviceChecker;
import info.varden.hauk.system.preferences.PreferenceManager;
import info.varden.hauk.system.preferences.ui.SettingsActivity;
import info.varden.hauk.ui.listener.AddLinkClickListener;
import info.varden.hauk.ui.listener.InitiateAdoptionClickListener;
import info.varden.hauk.ui.listener.SelectionModeChangedListener;
import info.varden.hauk.utils.DeprecationMigrator;
import info.varden.hauk.utils.Log;
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

        // Ensure that all deprecated preferences have been migrated before we continue.
        new DeprecationMigrator(this).migrate();

        Log.i("Creating main activity"); //NON-NLS
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.mainToolbar));

        setClassVariables();
        ((TextView) findViewById(R.id.labelAdoptWhatsThis)).setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        Log.d("Attaching event handlers"); //NON-NLS

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
                        Buttons.Two.YES_NO,
                        new ResumeDialogBuilder(response)
                );
            }
        });

        // Check for aggressive power saving devices and warn the user if applicable.
        new DeviceChecker(this).performCheck();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.title_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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
        PreferenceManager prefs = new PreferenceManager(this);

        // If there is an executable stop task, that means that sharing is already active. Shut down
        // the share by running the stop task instead of starting a new share.
        if (this.manager.isSessionActive()) {
            Log.i("Sharing is being stopped from main activity"); //NON-NLS
            stopSharing(prefs);
            return;
        }

        // Disable the UI while we attempt to connect to the Hauk backend.
        findViewById(R.id.btnShare).setEnabled(false);
        disableUI();

        String server = prefs.get(Constants.PREF_SERVER_ENCRYPTED).trim();
        String username = prefs.get(Constants.PREF_USERNAME_ENCRYPTED).trim();
        String password = prefs.get(Constants.PREF_PASSWORD_ENCRYPTED);
        int duration;
        int interval = prefs.get(Constants.PREF_INTERVAL);
        float minDistance = prefs.get(Constants.PREF_UPDATE_DISTANCE);
        String customID = prefs.get(Constants.PREF_CUSTOM_ID).trim();
        boolean useE2E = prefs.get(Constants.PREF_ENABLE_E2E);
        String e2ePass = !useE2E ? "" : prefs.get(Constants.PREF_E2E_PASSWORD);
        String nickname = ((TextView) findViewById(R.id.txtNickname)).getText().toString().trim();
        @SuppressWarnings("OverlyStrongTypeCast") ShareMode mode = ShareMode.fromMode(((Spinner) findViewById(R.id.selMode)).getSelectedItemPosition());
        String groupPin = ((TextView) findViewById(R.id.txtGroupCode)).getText().toString();
        boolean allowAdoption = ((Checkable) findViewById(R.id.chkAllowAdopt)).isChecked();
        @SuppressWarnings("OverlyStrongTypeCast") int durUnit = ((Spinner) findViewById(R.id.selUnit)).getSelectedItemPosition();

        try {
            // Try to parse the duration.
            duration = Integer.parseInt(((TextView) findViewById(R.id.txtDuration)).getText().toString());
            // The backend takes duration in seconds, hence it must be converted.
            duration = TimeUtils.timeUnitsToSeconds(duration, durUnit);
        } catch (NumberFormatException | ArithmeticException ex) {
            Log.e("Illegal duration value", ex); //NON-NLS
            this.dialogSvc.showDialog(R.string.err_client, R.string.err_invalid_duration, this.uiResetTask);
            return;
        }

        // Save connection preferences for next launch, so the user doesn't have to enter URL etc.
        // every time.
        Log.i("Updating connection preferences"); //NON-NLS
        prefs.set(Constants.PREF_DURATION, duration);
        prefs.set(Constants.PREF_DURATION_UNIT, durUnit);
        prefs.set(Constants.PREF_NICKNAME, nickname);
        prefs.set(Constants.PREF_ALLOW_ADOPTION, allowAdoption);

        if (server.isEmpty()) {
            // If the user hasn't set up a server yet, open the settings menu and prompt them to
            // configure the backend.
            this.uiResetTask.run();
            Toast.makeText(this, R.string.err_server_not_configured, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        assert mode != null;
        server = server.endsWith("/") ? server : server + "/";

        SessionInitiationPacket.InitParameters initParams = new SessionInitiationPacket.InitParameters(server, username, password, duration, interval, minDistance, customID, e2ePass);
        new ProxyHostnameResolverImpl(this, this.manager, this.uiResetTask, prefs, new SessionInitiationResponseHandlerImpl(), initParams, mode, allowAdoption, nickname, groupPin).resolve();
    }

    /**
     * Stops sharing. If the setting to prompt for confirmation is enabled, a dialog box is shown to
     * confirm that the share should be stopped.
     *
     * @param prefs A preference manager.
     */
    private void stopSharing(PreferenceManager prefs) {
        if (prefs.get(Constants.PREF_CONFIRM_STOP)) {
            this.dialogSvc.showDialog(R.string.dialog_confirm_stop_title, R.string.dialog_confirm_stop_body, Buttons.Three.YES_NO_REMEMBER, new StopSharingConfirmationPrompt(prefs, this.manager));
        } else {
            this.manager.stopSharing();
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
     * On-tap handler for the header logo and link that opens the Hauk project page on GitHub.
     */
    public void openProjectSite(View view) {
        new OpenLinkListener(this, R.string.label_source_link).onClick(view);
    }

    /**
     * This function is called by onCreate() to initialize class-level variables for usage in this
     * activity.
     */
    private void setClassVariables() {
        Log.d("Setting class variables"); //NON-NLS
        this.lockWhileRunning = new View[] {
                findViewById(R.id.txtDuration),

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
        ((TextView) findViewById(R.id.txtDuration)).setText(String.valueOf(prefs.get(Constants.PREF_DURATION)));
        ((TextView) findViewById(R.id.txtNickname)).setText(prefs.get(Constants.PREF_NICKNAME));
        // Because I can choose between an unchecked cast warning and an overly strong cast warning,
        // I'm going to with the latter.
        //noinspection OverlyStrongTypeCast
        ((Spinner) findViewById(R.id.selUnit)).setSelection(prefs.get(Constants.PREF_DURATION_UNIT));
        ((Checkable) findViewById(R.id.chkAllowAdopt)).setChecked(prefs.get(Constants.PREF_ALLOW_ADOPTION));

        // Set night mode preference.
        AppCompatDelegate.setDefaultNightMode(prefs.get(Constants.PREF_NIGHT_MODE).resolve());
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

        @Override
        public void onE2EForciblyDisabled(Version backendVersion) {
            MainActivity.this.dialogSvc.showDialog(R.string.err_outdated, String.format(getString(R.string.err_ver_e2e), Constants.VERSION_COMPAT_E2E_ENCRYPTION, backendVersion));
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
        public void onSessionCreated(Session session, final Share share, SessionInitiationReason reason) {
            // We now have a link to share, so we enable the additional link creation button if the backend supports it. Add an event handler to handle the user clicking on it.
            if (session.getBackendVersion().isAtLeast(Constants.VERSION_COMPAT_VIEW_ID)) {
                boolean allowNewLinkAdoption = ((Checkable) findViewById(R.id.chkAllowAdopt)).isChecked();
                Button btnLink = findViewById(R.id.btnLink);
                Log.d("Adding event handler for add-link button"); //NON-NLS
                btnLink.setOnClickListener(new AddLinkClickListener(MainActivity.this, session, allowNewLinkAdoption) {
                    @Override
                    public void onShareCreated(Share share) {
                        MainActivity.this.manager.shareLocation(share, SessionInitiationReason.SHARE_ADDED);
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

            // Service relaunches should be handled silently.
            if (reason == SessionInitiationReason.USER_STARTED) {
                MainActivity.this.dialogSvc.showDialog(R.string.ok_title, R.string.ok_message, Buttons.Two.OK_SHARE, new CustomDialogBuilder() {
                    @Override
                    public void onPositive() {
                        // OK button
                    }

                    @Override
                    public void onNegative() {
                        // Share button
                        Log.i("User requested to share %s", share); //NON-NLS
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType(Constants.INTENT_TYPE_COPY_LINK);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, MainActivity.this.getString(R.string.share_subject));
                        shareIntent.putExtra(Intent.EXTRA_TEXT, share.getViewURL());
                        MainActivity.this.startActivity(Intent.createChooser(shareIntent, MainActivity.this.getString(R.string.share_via)));
                    }

                    @Nullable
                    @Override
                    public View createView(Context ctx) {
                        return null;
                    }
                });
            }
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
