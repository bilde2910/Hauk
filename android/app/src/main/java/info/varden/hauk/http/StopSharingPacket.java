package info.varden.hauk.http;

import android.content.Context;

import info.varden.hauk.R;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.Version;
import info.varden.hauk.throwable.ServerException;

/**
 * Packet sent to tell the server to stop a single share or an entire session.
 */
public abstract class StopSharingPacket extends Packet {
    public abstract void onSuccess();

    /**
     * Creates a request to stop all shares for a given session.
     *
     * @param ctx     Android application context.
     * @param session The session to delete.
     */
    public StopSharingPacket(Context ctx, Session session) {
        super(ctx, session.getServerURL(), "api/stop.php");
        addParameter("sid", session.getID());
    }

    /**
     * Creates a request to stop a single share.
     *
     * @param ctx   Android application context.
     * @param share The share to stop.
     */
    public StopSharingPacket(Context ctx, Share share) {
        super(ctx, share.getSession().getServerURL(), "api/stop.php");
        addParameter("sid", share.getSession().getID());
        addParameter("lid", share.getID());
    }

    @Override
    protected final void onSuccess(String[] data, Version backendVersion) throws ServerException {
        // Somehow the data array can be empty? Check for this.
        if (data.length < 1) {
            throw new ServerException(getContext(), R.string.err_empty);
        }

        // All successful requests have OK as line 1.
        if (data[0].equals("OK")) {
            onSuccess();
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
}
