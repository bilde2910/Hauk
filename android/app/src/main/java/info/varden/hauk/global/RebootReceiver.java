package info.varden.hauk.global;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.global.ui.AuthorizationActivity;
import info.varden.hauk.global.ui.DisplayShareDialogListener;
import info.varden.hauk.global.ui.toast.GNSSStatusUpdateListenerImpl;
import info.varden.hauk.global.ui.toast.SessionInitiationResponseHandlerImpl;
import info.varden.hauk.global.ui.toast.ShareListenerImpl;
import info.varden.hauk.http.ConnectionParameters;
import info.varden.hauk.http.SessionInitiationPacket;
import info.varden.hauk.http.security.CertificateValidationPolicy;
import info.varden.hauk.manager.SessionManager;
import info.varden.hauk.struct.AdoptabilityPreference;
import info.varden.hauk.system.LocationPermissionsNotGrantedException;
import info.varden.hauk.system.LocationServicesDisabledException;
import info.varden.hauk.system.preferences.PreferenceManager;
import info.varden.hauk.utils.DeprecationMigrator;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.TimeUtils;
// Reboot broadcast receiver for Hauk. If enabled resumes a share session if one exists on device restart
public final class RebootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Subsequent calls may result in data being read from preferences. We should ensure that
        // all deprecated preferences have been migrated before we continue.
        new DeprecationMigrator(context).migrate();

        PreferenceManager prefs = new PreferenceManager(context);
        if(prefs.get(Constants.PREF_RESTART_ON_BOOT))
            resumeShare(context,intent);
    }
    private void resumeShare(Context context, Intent intent) {
        Log.d("Trying to resume shares...");
        new BroadcastSessionManager(context).resumeShares();
    }
}
