package info.varden.hauk.manager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import info.varden.hauk.Constants;
import info.varden.hauk.caching.ResumableSessions;
import info.varden.hauk.caching.ResumePrompt;
import info.varden.hauk.http.SessionInitiationPacket;
import info.varden.hauk.http.StopSharingPacket;
import info.varden.hauk.service.GNSSActiveHandler;
import info.varden.hauk.service.LocationPushService;
import info.varden.hauk.struct.AdoptabilityPreference;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.struct.Version;
import info.varden.hauk.system.LocationPermissionsNotGrantedException;
import info.varden.hauk.system.LocationServicesDisabledException;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.ReceiverDataRegistry;

/**
 * Manages sessions and shares.
 *
 * @author Marius Lindvall
 */
public abstract class SessionManager {
    /**
     * Request permission from the user to use location services.
     */
    protected abstract void requestLocationPermission();

    /**
     * A helper utility to resume sessions.
     */
    private final ResumableSessions resumable;

    /**
     * A handler that cancels the share after expiry.
     */
    private final Handler handler;

    /**
     * A runnable task that is executed when location sharing stops.
     */
    private final StopSharingTask stopTask;

    /**
     * A callback instance that is called upon when sharing is stopped, to reset the UI.
     */
    private final StopSharingCallback stopCallback;

    /**
     * Intent for the location pusher, so that it can be stopped if already running when launching
     * the app.
     */
    private static Intent pusher = null;

    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * A list of GNSS status update listeners that implementors have attached to this session
     * manager instance.
     */
    private final List<GNSSStatusUpdateListener> upstreamUpdateHandlers;

    /**
     * A list of share creation and removal listeners that implementors have attached to this
     * session manager instance.
     */
    private final List<ShareListener> upstreamShareListeners;

    /**
     * A list of session creation listeners that implementors have attached to this session manager
     * instance.
     */
    private final List<SessionListener> upstreamSessionListeners;

    /**
     * A list of shares that this session manager knows about. This is a mapping between share IDs
     * and the corresponding {@link Share} instances.
     */
    private final Map<String, Share> knownShares;

    /**
     * The currently active session. Used to check if a session has to be initialized when starting
     * a new share.
     */
    private Session activeSession = null;

    /**
     * Creates a new session manager with the given parameters.
     *
     * @param ctx          Android application context.
     * @param stopCallback A callback that is called when sharing sessions are stopped.
     */
    protected SessionManager(Context ctx, StopSharingCallback stopCallback) {
        this.ctx = ctx;
        this.stopCallback = stopCallback;

        this.upstreamUpdateHandlers = new ArrayList<>();
        this.upstreamShareListeners = new ArrayList<>();
        this.upstreamSessionListeners = new ArrayList<>();
        this.knownShares = new HashMap<>();

        this.resumable = new ResumableSessions(ctx);
        this.handler = new Handler();
        this.stopTask = new StopSharingTask(this.ctx, this.stopCallback) {
            @Override
            public void cleanup() {
                // Called when sharing ends. Clear the active session, and all collections of active
                // shares present in this class, then propagate this stop message upstream to all
                // session listeners.
                SessionManager.this.activeSession = null;
                SessionManager.this.handler.removeCallbacksAndMessages(null);
                SessionManager.this.resumable.clearResumableSession();
                for (GNSSStatusUpdateListener listener : SessionManager.this.upstreamUpdateHandlers) {
                    listener.onShutdown();
                }
            }
        };
    }

    /**
     * Returns whether or not there is currently an active session.
     */
    public final boolean isSessionActive() {
        return this.stopTask.canExecute();
    }

    /**
     * Request that the current sharing session is stopped.
     */
    public final void stopSharing() {
        this.stopTask.run();
    }

    /**
     * Attaches a GNSS status listener to the session manager. This listener is called whenever the
     * GNSS/location status updates.
     *
     * @param listener The listener to attach.
     */
    public final void attachStatusListener(GNSSStatusUpdateListener listener) {
        this.upstreamUpdateHandlers.add(listener);
    }

    /**
     * Attaches a share change listener to the session manager. This listener is called whenever a
     * share is started or stopped.
     *
     * @param listener The listener to attach.
     */
    public final void attachShareListener(ShareListener listener) {
        this.upstreamShareListeners.add(listener);
    }

    /**
     * Attaches a session creation listener to the session manager. This listener is called whenever
     * a session is created or fails to be created due to missing permissions.
     *
     * @param listener The listener to attach.
     */
    public final void attachSessionListener(SessionListener listener) {
        this.upstreamSessionListeners.add(listener);
    }

    /**
     * Attempts to resume shares.
     *
     * @param prompt A callback to prompt the user for whether or not they want to resume shares if
     *               any are found in storage.
     */
    public final void resumeShares(ResumePrompt prompt) {
        // Check if the location push service is already running. This will happen if the main UI
        // activity is killed/stopped, but the app itself and the pushing service keeps running in
        // the background. If this happens, the push service should be silently restarted to ensure
        // it behaves properly with new instances of GNSSActiveHandler and StopSharingTask that will
        // be created and attached when creating a new SessionManager in MainActivity. There is
        // probably a cleaner way to do this.
        if (pusher != null) {
            this.ctx.stopService(pusher);
            pusher = null;
            this.resumable.tryResumeShare(new ServiceRelauncher(this, this.resumable));
        } else {
            this.resumable.tryResumeShare(new AutoResumptionPrompter(this, this.resumable, prompt));
        }
    }

    /**
     * A preparation step for initiating sessions. Checks location services status and instantiates
     * a response handler for the session initiation packet.
     *
     * @param upstreamCallback An upstream callback to receive initiation progress updates.
     * @return A response handler for use with the {@link SessionInitiationPacket}.
     * @throws LocationServicesDisabledException if location services are disabled.
     * @throws LocationPermissionsNotGrantedException if location permissions have not been granted.
     */
    private SessionInitiationPacket.ResponseHandler preSessionInitiation(final SessionInitiationResponseHandler upstreamCallback, final SessionInitiationReason reason) throws LocationServicesDisabledException, LocationPermissionsNotGrantedException {
        // Check for location permission and prompt the user if missing. This returns because the
        // checking function creates async dialogs here - the user is prompted to press the button
        // again instead.
        if (!hasLocationPermission()) throw new LocationPermissionsNotGrantedException();
        LocationManager locMan = (LocationManager) this.ctx.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsDisabled = locMan != null && !locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (gpsDisabled) throw new LocationServicesDisabledException();

        // Tell the upstream listener that we are now initiating the packet.
        upstreamCallback.onInitiating();

        // Create a handler for our request to initiate a new session. This is declared separately
        // from the SessionInitiationPackets below to avoid code duplication.
        Log.i("Creating session initiation response handler"); //NON-NLS
        return new SessionInitiationPacket.ResponseHandler() {
            @Override
            public void onSessionInitiated(Share share) {
                Log.i("Session was initiated for share %s; setting session resumable", share); //NON-NLS

                // Proceed with the location share.
                shareLocation(share, reason);

                upstreamCallback.onSuccess();
            }

            @Override
            public void onShareModeIncompatible(ShareMode downgradeTo, Version backendVersion) {
                Log.e("The requested sharing mode is incompatible because the server is out of date (backend=%s)", backendVersion); //NON-NLS
                upstreamCallback.onShareModeForciblyDowngraded(downgradeTo, backendVersion);
            }

            @Override
            public void onE2EUnavailable(Version backendVersion) {
                Log.e("End-to-end encryption was requested but dropped because the server is out of date (backend=%s)", backendVersion); //NON-NLS
                upstreamCallback.onE2EForciblyDisabled(backendVersion);
            }

            @Override
            public void onFailure(Exception ex) {
                Log.e("Share could not be initiated", ex); //NON-NLS
                upstreamCallback.onFailure(ex);
            }
        };
    }

    /**
     * Starts a single-user sharing session.
     *
     * @param initParams       Connection parameters describing the backend server to connect to.
     * @param upstreamCallback An upstream callback to receive initiation progress updates.
     * @param allowAdoption    Whether or not to allow this share to be adopted.
     * @throws LocationServicesDisabledException if location services are disabled.
     * @throws LocationPermissionsNotGrantedException if location permissions have not been granted.
     */
    public final void shareLocation(SessionInitiationPacket.InitParameters initParams, SessionInitiationResponseHandler upstreamCallback, AdoptabilityPreference allowAdoption) throws LocationPermissionsNotGrantedException, LocationServicesDisabledException {
        SessionInitiationPacket.ResponseHandler handler = preSessionInitiation(upstreamCallback, SessionInitiationReason.USER_STARTED);

        // Create a handshake request and handle the response. The handshake transmits the duration
        // and interval to the server and waits for the server to return a session ID to confirm
        // session creation.
        Log.i("Creating single-user session initiation packet"); //NON-NLS
        new SessionInitiationPacket(this.ctx, initParams, handler, allowAdoption).send();
    }

    /**
     * Starts a group sharing session.
     *
     * @param initParams       Connection parameters describing the backend server to connect to.
     * @param upstreamCallback An upstream callback to receive initiation progress updates.
     * @param nickname         The nickname to use on the map.
     * @throws LocationServicesDisabledException if location services are disabled.
     * @throws LocationPermissionsNotGrantedException if location permissions have not been granted.
     */
    public final void shareLocation(SessionInitiationPacket.InitParameters initParams, SessionInitiationResponseHandler upstreamCallback, String nickname) throws LocationPermissionsNotGrantedException, LocationServicesDisabledException {
        SessionInitiationPacket.ResponseHandler handler = preSessionInitiation(upstreamCallback, SessionInitiationReason.USER_STARTED);

        // Create a handshake request and handle the response. The handshake transmits the duration
        // and interval to the server and waits for the server to return a session ID to confirm
        // session creation.
        Log.i("Creating group session initiation packet"); //NON-NLS
        new SessionInitiationPacket(this.ctx, initParams, handler, nickname).send();
    }

    /**
     * Joins an existing group sharing session.
     *
     * @param initParams       Connection parameters describing the backend server to connect to.
     * @param upstreamCallback An upstream callback to receive initiation progress updates.
     * @param nickname         The nickname to use on the map.
     * @param groupPin         The join code of the group to join.
     * @throws LocationServicesDisabledException if location services are disabled.
     * @throws LocationPermissionsNotGrantedException if location permissions have not been granted.
     */
    public final void shareLocation(SessionInitiationPacket.InitParameters initParams, SessionInitiationResponseHandler upstreamCallback, String nickname, String groupPin) throws LocationPermissionsNotGrantedException, LocationServicesDisabledException {
        SessionInitiationPacket.ResponseHandler handler = preSessionInitiation(upstreamCallback, SessionInitiationReason.USER_STARTED);

        // Create a handshake request and handle the response. The handshake transmits the duration
        // and interval to the server and waits for the server to return a session ID to confirm
        // session creation.
        Log.i("Creating group session join packet"); //NON-NLS
        new SessionInitiationPacket(this.ctx, initParams, handler, nickname, groupPin).send();
    }

    /**
     * Executes a location sharing session against the server. This can be a new session, or a
     * resumed session.
     *
     * @param share The share to run against the server.
     */
    public final void shareLocation(Share share, SessionInitiationReason reason) {
        // If we are not already sharing our location, initiate a new session.
        if (this.activeSession == null) {
            initiateSessionForExistingShare(share, reason);
        }

        Log.i("Attaching to share, share=%s", share); //NON-NLS
        this.resumable.setShareResumable(share);
        this.knownShares.put(share.getID(), share);

        for (ShareListener listener : this.upstreamShareListeners) {
            listener.onShareJoined(share);
        }
    }

    /**
     * Requests that a single share is stopped.
     *
     * @param share The share to stop.
     */
    public final void stopSharing(final Share share) {
        new StopSharingPacket(this.ctx, share) {
            @Override
            public void onSuccess() {
                Log.i("Share %s was successfully stopped", share); //NON-NLS
                SessionManager.this.resumable.clearResumableShare(share.getID());
                SessionManager.this.knownShares.remove(share.getID());
                for (ShareListener listener : SessionManager.this.upstreamShareListeners) {
                    listener.onShareParted(share);
                }
            }

            @Override
            protected void onFailure(Exception ex) {
                Log.e("Share %s could not be stopped", ex, share); //NON-NLS
            }
        }.send();
    }

    /**
     * For internal use only. Spawns a new location push service that actually sends location data
     * to the backend.
     *
     * @param share The share whose session should be pushed to.
     */
    private void initiateSessionForExistingShare(Share share, SessionInitiationReason reason) {
        this.activeSession = share.getSession();
        this.resumable.setSessionResumable(this.activeSession);

        // Even though we previously requested location permission, we still have to check for it
        // when we actually use the location API (user could have disabled it while connecting).
        if (this.ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i("Location permission has been granted; sharing will commence"); //NON-NLS
            GNSSActiveHandler statusUpdateHandler = new GNSSStatusUpdateTask(share.getSession());

            // Create a client that receives location updates and pushes these to
            // the Hauk backend.
            Log.d("Creating location push service intent"); //NON-NLS
            Intent pusher = new Intent(this.ctx, LocationPushService.class);
            pusher.setAction(LocationPushService.ACTION_ID);
            pusher.putExtra(Constants.EXTRA_SHARE, ReceiverDataRegistry.register(share));
            pusher.putExtra(Constants.EXTRA_STOP_TASK, ReceiverDataRegistry.register(this.stopTask));
            pusher.putExtra(Constants.EXTRA_HANDLER, ReceiverDataRegistry.register(this.handler));
            pusher.putExtra(Constants.EXTRA_GNSS_ACTIVE_TASK, ReceiverDataRegistry.register(statusUpdateHandler));

            // Android O and higher require the service to be started as a foreground service for it
            // not to be killed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.i("Starting location pusher as foreground service"); //NON-NLS
                this.ctx.startForegroundService(pusher);
            } else {
                Log.i("Starting location pusher as service"); //NON-NLS
                this.ctx.startService(pusher);
            }

            // When both the notification and pusher are created, we can update the stop task with
            // these so that they can be canceled when the location share ends.
            this.stopTask.updateTask(pusher);

            // Required for session relaunches
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            SessionManager.pusher = pusher;

            // stopTask is scheduled for expiration, but it could also be called if the user
            // manually stops the share, or if the app is destroyed.
            long expireIn = share.getSession().getRemainingMillis();
            Log.i("Scheduling session expiration in %s milliseconds", expireIn); //NON-NLS
            this.handler.postDelayed(this.stopTask, expireIn);

            // Push the start event to upstream listeners.
            for (GNSSStatusUpdateListener listener : this.upstreamUpdateHandlers) {
                listener.onStarted();
            }
            for (SessionListener listener : this.upstreamSessionListeners) {
                listener.onSessionCreated(share.getSession(), share, reason);
            }
        } else {
            Log.w("Location permission has not been granted; sharing will not commence"); //NON-NLS
            for (SessionListener listener : this.upstreamSessionListeners) {
                listener.onSessionCreationFailedDueToPermissions();
            }
        }
    }

    /**
     * Checks whether or not the user granted Hauk permission to use their device location. If
     * permission has not been granted, this function creates a dialog which runs asynchronously,
     * meaning this function does not wait until permission has been granted before it returns.
     *
     * @return true if permission is granted, false if the user needs to be asked.
     */
    private boolean hasLocationPermission() {
        if (this.ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Location permission has not been granted. Asking user for permission"); //NON-NLS
            requestLocationPermission();
            return false;
        } else {
            Log.i("Location permission check was successful"); //NON-NLS
            return true;
        }
    }

    /**
     * The GNSS status handler that the {@link SessionManager} itself uses to receive status updates
     * from the GNSS listeners. Used to propagate events further upstream.
     */
    private final class GNSSStatusUpdateTask implements GNSSActiveHandler {
        /**
         * The session that we are receiving status updates for.
         */
        private final Session session;

        private GNSSStatusUpdateTask(Session session) {
            this.session = session;
        }

        @Override
        public void onCoarseRebound() {
            for (GNSSStatusUpdateListener listener : SessionManager.this.upstreamUpdateHandlers) {
                listener.onGNSSConnectionLost();
            }
        }

        @Override
        public void onCoarseLocationReceived() {
            for (GNSSStatusUpdateListener listener : SessionManager.this.upstreamUpdateHandlers) {
                listener.onCoarseLocationReceived();
            }
        }

        @Override
        public void onAccurateLocationReceived() {
            for (GNSSStatusUpdateListener listener : SessionManager.this.upstreamUpdateHandlers) {
                listener.onAccurateLocationReceived();
            }
        }

        @Override
        public void onServerConnectionLost() {
            for (GNSSStatusUpdateListener listener : SessionManager.this.upstreamUpdateHandlers) {
                listener.onServerConnectionLost();
            }
        }

        @Override
        public void onServerConnectionRestored() {
            for (GNSSStatusUpdateListener listener : SessionManager.this.upstreamUpdateHandlers) {
                listener.onServerConnectionRestored();
            }
        }

        @Override
        public void onShareListReceived(String linkFormat, String[] shareIDs) {
            List<String> currentShares = Arrays.asList(shareIDs);
            for (int i = 0; i < currentShares.size(); i++) {
                String shareID = currentShares.get(i);
                if (!SessionManager.this.knownShares.containsKey(shareID)) {
                    // A new share has been added. If the client is suddenly informed of a new
                    // share, it is always a group share because that is the only type of shares
                    // that can be initiated by a remote user (through adoption).
                    Share newShare = new Share(this.session, String.format(linkFormat, shareID), shareID, ShareMode.JOIN_GROUP);
                    Log.i("Received unknown share %s from server", newShare); //NON-NLS
                    shareLocation(newShare, SessionInitiationReason.SHARE_ADDED);
                }
            }
            for (Iterator<Map.Entry<String, Share>> it = SessionManager.this.knownShares.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Share> entry = it.next();
                if (!currentShares.contains(entry.getKey())) {
                    // A share has been removed.
                    Log.i("Share %s was terminated on server, removing", entry.getKey()); //NON-NLS
                    it.remove();
                    SessionManager.this.resumable.clearResumableShare(entry.getKey());
                    for (ShareListener listener : SessionManager.this.upstreamShareListeners) {
                        listener.onShareParted(entry.getValue());
                    }
                }
            }
        }
    }
}
