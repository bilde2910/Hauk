package info.varden.hauk.http;

import android.content.Context;

import info.varden.hauk.HaukConst;
import info.varden.hauk.R;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.struct.Version;
import info.varden.hauk.throwable.ServerException;
import info.varden.hauk.utils.TimeUtils;

/**
 * Packet sent to initiate a sharing session on the server. Creates a share of a given type.
 */
public abstract class SessionInitiationPacket extends Packet {
    protected abstract ResponseHandler getHandler();

    private final String server;
    private final int durationSec;
    private final int interval;

    private ShareMode mode;

    private SessionInitiationPacket(Context ctx, String server, String password, int durationSec, int interval) {
        super(ctx, server, HaukConst.URL_PATH_CREATE_SHARE);
        this.server = server;
        this.durationSec = durationSec;
        this.interval = interval;
        addParameter(HaukConst.PACKET_PARAM_PASSWORD, password);
        addParameter(HaukConst.PACKET_PARAM_DURATION, String.valueOf(durationSec));
        addParameter(HaukConst.PACKET_PARAM_INTERVAL, String.valueOf(interval));
    }

    /**
     * Creates a packet and designates it as a request to create a single-user share.
     *
     * @param ctx           Android application context.
     * @param server        The full base URL for the Hauk server.
     * @param password      The password defined in the backend config.
     * @param durationSec   The duration of the sharing session, in seconds.
     * @param interval      The interval between each location update, in seconds.
     * @param allowAdoption Whether or not to allow this share to be adopted into a group share.
     */
    public SessionInitiationPacket(Context ctx, String server, String password, int durationSec, int interval, boolean allowAdoption) {
        this(ctx, server, password, durationSec, interval);
        this.mode = ShareMode.CREATE_ALONE;
        addParameter(HaukConst.PACKET_PARAM_SHARE_MODE, String.valueOf(this.mode.getMode()));
        addParameter(HaukConst.PACKET_PARAM_ADOPTABLE, allowAdoption ? "1" : "0");
    }

    /**
     * Creates a packet and designates it as a request to create a group share.
     *
     * @param ctx         Android application context.
     * @param server      The full base URL for the Hauk server.
     * @param password    The password defined in the backend config.
     * @param durationSec The duration of the sharing session, in seconds.
     * @param interval    The interval between each location update, in seconds.
     * @param nickname    The nickname to display on the map.
     */
    public SessionInitiationPacket(Context ctx, String server, String password, int durationSec, int interval, String nickname) {
        this(ctx, server, password, durationSec, interval);
        this.mode = ShareMode.CREATE_GROUP;
        addParameter(HaukConst.PACKET_PARAM_SHARE_MODE, String.valueOf(this.mode.getMode()));
        addParameter(HaukConst.PACKET_PARAM_NICKNAME, nickname);
    }

    /**
     * Creates a packet and designates it as a request to join an existing group share.
     *
     * @param ctx         Android application context.
     * @param server      The full base URL for the Hauk server.
     * @param password    The password defined in the backend config.
     * @param durationSec The duration of the sharing session, in seconds.
     * @param interval    The interval between each location update, in seconds.
     * @param nickname    The nickname to display on the map.
     * @param groupPin    The PIN code to join the group.
     */
    public SessionInitiationPacket(Context ctx, String server, String password, int durationSec, int interval, String nickname, String groupPin) {
        this(ctx, server, password, durationSec, interval);
        this.mode = ShareMode.JOIN_GROUP;
        addParameter(HaukConst.PACKET_PARAM_SHARE_MODE, String.valueOf(this.mode.getMode()));
        addParameter(HaukConst.PACKET_PARAM_NICKNAME, nickname);
        addParameter(HaukConst.PACKET_PARAM_GROUP_PIN, groupPin);
    }

    @Override
    protected final void onSuccess(String[] data, Version backendVersion) throws ServerException {
        // Check if the server is out of date for group shares, if applicable.
        if (this.mode.isGroupType()) {
            if (!backendVersion.atLeast(HaukConst.VERSION_COMPAT_GROUP_SHARE)) {
                // If the server is indeed out of date, override the sharing mode to reflect what
                // was actually created on the server.
                this.mode = ShareMode.CREATE_ALONE;
                getHandler().onShareModeIncompatible(backendVersion);
            }
        }

        // Somehow the data array can be empty? Check for this.
        if (data.length < 1) {
            throw new ServerException(getContext(), R.string.err_empty);
        }

        // A successful session initiation contains "OK" on line 1, the session ID on line 2, and a
        // publicly sharable tracking link on line 3.
        if (data[0].equals(HaukConst.PACKET_RESPONSE_OK)) {
            String sessionID = data[1];
            String viewURL = data[2];
            String joinCode = null;
            String viewID = viewURL;

            // If the share is compatible, fetch the group join code.
            if (this.mode == ShareMode.CREATE_GROUP) {
                joinCode = data[3];
            }

            // If the server sends it, get the internal share ID as well for the list of currently
            // active shares in the UI. It is better UX to display this instead of the full URL in
            // the list, but fall back to the full URL if needed.
            if (backendVersion.atLeast(HaukConst.VERSION_COMPAT_VIEW_ID)) {
                if (this.mode == ShareMode.CREATE_GROUP) {
                    viewID = data[4];
                } else {
                    viewID = data[3];
                }
            }

            // Create a share and pass it upstream.
            Session session = new Session(this.server, backendVersion, sessionID, (long) this.durationSec * (long) TimeUtils.MILLIS_PER_SECOND + System.currentTimeMillis(), this.interval);
            Share share = new Share(session, viewURL, viewID, joinCode, this.mode);

            getHandler().onSessionInitiated(share);
        } else {
            // If the first line of the response is not "OK", an error of some sort has occurred and
            // should be displayed to the user.
            StringBuilder err = new StringBuilder();
            for (String line : data) {
                err.append(line);
                err.append("\n");
            }
            throw new ServerException(err.toString());
        }
    }

    @Override
    protected final void onFailure(Exception ex) {
        getHandler().onFailure(ex);
    }

    /**
     * In order to avoid code duplication, we request a common handler that will be used and whose
     * methods will be called instead of abstract methods directly in the packet class. This allows
     * us to have a single instance of the handler instead of having to reimplement the handlers for
     * all types of shares.
     */
    public interface ResponseHandler {
        void onSessionInitiated(Share share);
        void onFailure(Exception ex);
        void onShareModeIncompatible(Version backendVersion);
    }
}
