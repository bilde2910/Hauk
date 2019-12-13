package info.varden.hauk.http;

import android.content.Context;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.Version;

/**
 * Packet sent to tell the server to stop a single share or an entire session.
 *
 * @author Marius Lindvall
 */
public abstract class StopSharingPacket extends Packet {
    /**
     * Called if the share was successfully stopped.
     */
    protected abstract void onSuccess();

    /**
     * Creates a request to stop all shares for a given session.
     *
     * @param ctx     Android application context.
     * @param session The session to delete.
     */
    protected StopSharingPacket(Context ctx, Session session) {
        super(ctx, session.getServerURL(), session.getConnectionParameters(), Constants.URL_PATH_STOP_SHARING);
        setParameter(Constants.PACKET_PARAM_SESSION_ID, session.getID());
    }

    /**
     * Creates a request to stop a single share.
     *
     * @param ctx   Android application context.
     * @param share The share to stop.
     */
    protected StopSharingPacket(Context ctx, Share share) {
        super(ctx, share.getSession().getServerURL(), share.getSession().getConnectionParameters(), Constants.URL_PATH_STOP_SHARING);
        setParameter(Constants.PACKET_PARAM_SESSION_ID, share.getSession().getID());
        setParameter(Constants.PACKET_PARAM_SHARE_ID, share.getID());
    }

    @Override
    protected final void onSuccess(String[] data, Version backendVersion) throws ServerException {
        // Somehow the data array can be empty? Check for this.
        if (data.length < 1) {
            throw new ServerException(getContext(), R.string.err_empty);
        }

        // All successful requests have OK as line 1.
        if (data[0].equals(Constants.PACKET_RESPONSE_OK)) {
            onSuccess();
        } else {
            // If the first line of the response is not "OK", an error of some sort has occurred and
            // should be displayed to the user.
            StringBuilder err = new StringBuilder();
            for (String line : data) {
                err.append(line);
                err.append(System.lineSeparator());
            }
            throw new ServerException(err.toString());
        }
    }
}
