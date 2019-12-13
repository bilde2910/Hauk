package info.varden.hauk.http;

import android.content.Context;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.struct.ShareMode;
import info.varden.hauk.struct.Version;

/**
 * Packet that is sent to create a new single-user sharing link for an already running session.
 *
 * @author Marius Lindvall
 */
public abstract class NewLinkPacket extends Packet {
    /**
     * Called if the new share link was successfully created.
     *
     * @param share The share that was created.
     */
    protected abstract void onShareCreated(Share share);

    /**
     * The session for which the sharing link should be created.
     */
    private final Session session;

    /**
     * Creates the packet.
     *
     * @param ctx           Android application context.
     * @param session       The session to create a new sharing link for.
     * @param allowAdoption Whether or not this share should be adoptable.
     */
    protected NewLinkPacket(Context ctx, Session session, boolean allowAdoption) {
        super(ctx, session.getServerURL(), session.getConnectionParameters(), Constants.URL_PATH_CREATE_NEW_LINK);
        this.session = session;
        setParameter(Constants.PACKET_PARAM_SESSION_ID, session.getID());
        setParameter(Constants.PACKET_PARAM_ADOPTABLE, allowAdoption ? "1" : "0");
    }

    @Override
    protected final void onSuccess(String[] data, Version backendVersion) throws ServerException {
        // Somehow the data array can be empty.
        if (data.length < 1) {
            throw new ServerException(getContext(), R.string.err_empty);
        }

        // A successful session initiation contains "OK" on line 1, the publicly sharable tracking
        // link on line 2, and its ID on line 3.
        if (data[0].equals(Constants.PACKET_RESPONSE_OK)) {
            String viewLink = data[1];
            String viewID = data[2];
            onShareCreated(new Share(this.session, viewLink, viewID, ShareMode.CREATE_ALONE));

        } else {
            // If the first line of the response is not "OK", an error
            // of some sort has occurred and should be displayed to the
            // user.
            StringBuilder err = new StringBuilder();
            for (String line : data) {
                err.append(line);
                err.append(System.lineSeparator());
            }
            throw new ServerException(err.toString());
        }
    }
}
