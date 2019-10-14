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
    private static final String ACTION_START_SHARING_ALONE = "info.varden.hauk.START_ALONE_THEN_SHARE_VIA";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Ensure we have a valid broadcast.
        if (!ACTION_START_SHARING_ALONE.equals(intent.getAction())) return;
        if (!intent.hasExtra(Constants.EXTRA_BROADCAST_AUTHORIZATION_IDENTIFIER)) return;

        // Check that the source is authorized.
        String identifier = intent.getStringExtra(Constants.EXTRA_BROADCAST_AUTHORIZATION_IDENTIFIER);
        SharedPreferences authPrefs = context.getSharedPreferences(Constants.SHARED_PREFS_AUTHORIZATIONS, Context.MODE_PRIVATE);
        if (!authPrefs.contains(identifier)) {
            // If not, show the authorization dialog and abort the session initiation.
            Intent authIntent = new Intent(context, AuthorizationActivity.class);
            authIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            authIntent.putExtra(Constants.EXTRA_BROADCAST_AUTHORIZATION_IDENTIFIER, identifier);
            context.startActivity(authIntent);
            return;
        } else if (!authPrefs.getBoolean(identifier, false)) {
            // Denied sources are silently ignored.
            return;
        }

        // Create session initiation parameters.
        PreferenceManager prefs = new PreferenceManager(context);
        SessionInitiationPacket.InitParameters initParams = buildSessionParams(intent, prefs);
        boolean adoptable = intent.hasExtra(Constants.EXTRA_SESSION_ALLOW_ADOPT) ? intent.getBooleanExtra(Constants.EXTRA_SESSION_ALLOW_ADOPT, true) : prefs.get(Constants.PREF_ALLOW_ADOPTION);

        SessionManager manager = new BroadcastSessionManager(context);
        manager.attachShareListener(new DisplayShareDialogListener(context));
        manager.attachStatusListener(new GNSSStatusUpdateListenerImpl(context));

        try {
            manager.shareLocation(initParams, new SessionInitiationResponseHandlerImpl(context), adoptable ? AdoptabilityPreference.ALLOW_ADOPTION : AdoptabilityPreference.DISALLOW_ADOPTION);
        } catch (LocationPermissionsNotGrantedException e) {
            Toast.makeText(context, R.string.err_missing_perms, Toast.LENGTH_LONG).show();
        } catch (LocationServicesDisabledException e) {
            Toast.makeText(context, R.string.err_location_disabled, Toast.LENGTH_LONG).show();
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
        String password = intent.hasExtra(Constants.EXTRA_SESSION_PASSWORD) ? intent.getStringExtra(Constants.EXTRA_SESSION_PASSWORD) : fallback.get(Constants.PREF_PASSWORD);
        int duration = intent.hasExtra(Constants.EXTRA_SESSION_DURATION) ? intent.getIntExtra(Constants.EXTRA_SESSION_DURATION, 0) : TimeUtils.timeUnitsToSeconds(fallback.get(Constants.PREF_DURATION), fallback.get(Constants.PREF_DURATION_UNIT));
        int interval = intent.hasExtra(Constants.EXTRA_SESSION_INTERVAL) ? intent.getIntExtra(Constants.EXTRA_SESSION_INTERVAL, 0) : fallback.get(Constants.PREF_INTERVAL);

        assert server != null;
        server = server.endsWith("/") ? server : server + "/";

        return new SessionInitiationPacket.InitParameters(server, password, duration, interval);
    }
}
