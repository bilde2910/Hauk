package info.varden.hauk.global;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.global.ui.AuthorizationActivity;
import info.varden.hauk.global.ui.DisplayShareDialogListener;
import info.varden.hauk.global.ui.toast.GNSSStatusUpdateListenerImpl;
import info.varden.hauk.global.ui.toast.SessionInitiationResponseHandlerImpl;
import info.varden.hauk.global.ui.toast.ShareListenerImpl;
import info.varden.hauk.http.SessionInitiationPacket;
import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.struct.AdoptabilityPreference;
import info.varden.hauk.system.LocationPermissionsNotGrantedException;
import info.varden.hauk.system.LocationServicesDisabledException;
import info.varden.hauk.utils.PreferenceManager;
import info.varden.hauk.utils.TimeUtils;

/**
 * Public broadcast receiver for Hauk. Allows creation of new sessions from broadcasts. Broadcasts
 * sent to this receiver may declare the following extras:
 *
 * <p>For info.varden.hauk.START_ALONE_THEN_SHARE_VIA:</p>
 * <ul>
 *     <li><b>source: </b><i>(required)</i>
 *          An identifier for the broadcast source, e.g. package name of source app.</li>
 *     <li><b>server: </b><i>(optional)</i>
 *          The Hauk backend to connect to. Defaults to saved preference.</li>
 *     <li><b>password: </b><i>(optional)</i>
 *          The backend password. Defaults to saved preference.</li>
 *     <li><b>duration: </b><i>(optional)</i>
 *          Number of seconds to share for. Defaults to saved preference.</li>
 *     <li><b>interval: </b><i>(optional)</i>
 *          Number of seconds between each update. Defaults to saved preference.</li>
 *     <li><b>adoptable: </b><i>(optional)</i>
 *          True or false for allowing share adoption. Defaults to saved preference.</li>
 * </ul>
 *
 * @since 1.3
 * @author Marius Lindvall
 */
public final class Receiver extends BroadcastReceiver {
    @SuppressWarnings("HardCodedStringLiteral")
    private static final String ACTION_START_SHARING_ALONE_WITH_MENU = "info.varden.hauk.START_ALONE_THEN_SHARE_VIA";
    @SuppressWarnings("HardCodedStringLiteral")
    private static final String ACTION_START_SHARING_ALONE_WITH_TOAST = "info.varden.hauk.START_ALONE_THEN_MAKE_TOAST";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Ensure we have a valid broadcast.
        if (intent.getAction() == null) return;
        if (!intent.hasExtra(Constants.EXTRA_BROADCAST_AUTHORIZATION_IDENTIFIER)) return;

        // Check that the broadcast is authorized.
        if (!checkAuthorization(context, intent)) return;

        // Handle the broadcast appropriately.
        switch (intent.getAction()) {
            case ACTION_START_SHARING_ALONE_WITH_MENU:
                startAloneThenShareVia(context, intent);
                break;
            case ACTION_START_SHARING_ALONE_WITH_TOAST:
                startAloneThenMakeToast(context, intent);
                break;
        }
    }

    /**
     * Checks whether or not the given broadcast source is authorized to start sharing without user
     * interaction, and prompts the user if this status is unknown.
     *
     * @param ctx    Android application context.
     * @param intent The broadcast intent.
     * @return true if handling should continue; false otherwise.
     */
    private static boolean checkAuthorization(Context ctx, Intent intent) {
        // Check that the source is authorized.
        String identifier = intent.getStringExtra(Constants.EXTRA_BROADCAST_AUTHORIZATION_IDENTIFIER);
        SharedPreferences authPrefs = ctx.getSharedPreferences(Constants.SHARED_PREFS_AUTHORIZATIONS, Context.MODE_PRIVATE);
        if (!authPrefs.contains(identifier)) {
            // If not, show the authorization dialog and abort the session initiation.
            Intent authIntent = new Intent(ctx, AuthorizationActivity.class);
            authIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            authIntent.putExtra(Constants.EXTRA_BROADCAST_AUTHORIZATION_IDENTIFIER, identifier);
            ctx.startActivity(authIntent);
            return false;
        } else if (!authPrefs.getBoolean(identifier, false)) {
            // Denied sources are silently ignored.
            return false;
        } else {
            // Proceed with broadcast handling.
            return true;
        }
    }

    /**
     * Starts a single user sharing session and prompts the user to share the link.
     *
     * @param ctx    Android application context.
     * @param intent The broadcast intent.
     */
    private static void startAloneThenShareVia(Context ctx, Intent intent) {
        // Create session initiation parameters.
        PreferenceManager prefs = new PreferenceManager(ctx);
        SessionInitiationPacket.InitParameters initParams = buildSessionParams(intent, prefs);
        boolean adoptable = intent.hasExtra(Constants.EXTRA_SESSION_ALLOW_ADOPT) ? intent.getBooleanExtra(Constants.EXTRA_SESSION_ALLOW_ADOPT, true) : prefs.get(Constants.PREF_ALLOW_ADOPTION);

        SessionManager manager = new BroadcastSessionManager(ctx);
        manager.attachShareListener(new DisplayShareDialogListener(ctx));
        manager.attachStatusListener(new GNSSStatusUpdateListenerImpl(ctx));

        try {
            manager.shareLocation(initParams, new SessionInitiationResponseHandlerImpl(ctx), adoptable ? AdoptabilityPreference.ALLOW_ADOPTION : AdoptabilityPreference.DISALLOW_ADOPTION);
        } catch (LocationPermissionsNotGrantedException e) {
            Toast.makeText(ctx, R.string.err_missing_perms, Toast.LENGTH_LONG).show();
        } catch (LocationServicesDisabledException e) {
            Toast.makeText(ctx, R.string.err_location_disabled, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Starts a single user sharing session and displays the sharing link in a toast notification.
     *
     * @param ctx    Android application context.
     * @param intent The broadcast intent.
     */
    private static void startAloneThenMakeToast(Context ctx, Intent intent) {
        // Require a custom link ID for this broadcast.
        if (!intent.hasExtra(Constants.EXTRA_SESSION_CUSTOM_ID)) return;

        // Create session initiation parameters.
        PreferenceManager prefs = new PreferenceManager(ctx);
        SessionInitiationPacket.InitParameters initParams = buildSessionParams(intent, prefs);
        boolean adoptable = intent.hasExtra(Constants.EXTRA_SESSION_ALLOW_ADOPT) ? intent.getBooleanExtra(Constants.EXTRA_SESSION_ALLOW_ADOPT, true) : prefs.get(Constants.PREF_ALLOW_ADOPTION);

        SessionManager manager = new BroadcastSessionManager(ctx);
        manager.attachShareListener(new ShareListenerImpl(ctx));
        manager.attachStatusListener(new GNSSStatusUpdateListenerImpl(ctx));

        try {
            manager.shareLocation(initParams, new SessionInitiationResponseHandlerImpl(ctx), adoptable ? AdoptabilityPreference.ALLOW_ADOPTION : AdoptabilityPreference.DISALLOW_ADOPTION);
        } catch (LocationPermissionsNotGrantedException e) {
            Toast.makeText(ctx, R.string.err_missing_perms, Toast.LENGTH_LONG).show();
        } catch (LocationServicesDisabledException e) {
            Toast.makeText(ctx, R.string.err_location_disabled, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Creates session initiation parameters from broadcast intent data.
     *
     * @param intent   The intent to extract data from.
     * @param fallback A preference manager to fetch default values from.
     * @return Session initiation parameters.
     */
    private static SessionInitiationPacket.InitParameters buildSessionParams(Intent intent, PreferenceManager fallback) {
        String server = intent.hasExtra(Constants.EXTRA_SESSION_SERVER_URL) ? intent.getStringExtra(Constants.EXTRA_SESSION_SERVER_URL) : fallback.get(Constants.PREF_SERVER);
        String username = intent.hasExtra(Constants.EXTRA_SESSION_USERNAME) ? intent.getStringExtra(Constants.EXTRA_SESSION_USERNAME) : fallback.get(Constants.PREF_USERNAME);
        String password = intent.hasExtra(Constants.EXTRA_SESSION_PASSWORD) ? intent.getStringExtra(Constants.EXTRA_SESSION_PASSWORD) : fallback.get(Constants.PREF_PASSWORD);
        int duration = intent.hasExtra(Constants.EXTRA_SESSION_DURATION) ? intent.getIntExtra(Constants.EXTRA_SESSION_DURATION, 0) : TimeUtils.timeUnitsToSeconds(fallback.get(Constants.PREF_DURATION), fallback.get(Constants.PREF_DURATION_UNIT));
        int interval = intent.hasExtra(Constants.EXTRA_SESSION_INTERVAL) ? intent.getIntExtra(Constants.EXTRA_SESSION_INTERVAL, 0) : fallback.get(Constants.PREF_INTERVAL);
        String customID = intent.hasExtra(Constants.EXTRA_SESSION_CUSTOM_ID) ? intent.getStringExtra(Constants.EXTRA_SESSION_CUSTOM_ID) : fallback.get(Constants.PREF_CUSTOM_ID);

        assert server != null;
        server = server.endsWith("/") ? server : server + "/";

        return new SessionInitiationPacket.InitParameters(server, username, password, duration, interval, customID);
    }
}
