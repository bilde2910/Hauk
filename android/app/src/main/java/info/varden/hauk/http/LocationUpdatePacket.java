package info.varden.hauk.http;

import android.content.Context;
import android.location.Location;

import info.varden.hauk.HaukConst;
import info.varden.hauk.R;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Version;
import info.varden.hauk.throwable.ServerException;

/**
 * Packet that is sent to update the client's location on the map.
 *
 * @author Marius Lindvall
 */
public abstract class LocationUpdatePacket extends Packet {
    public abstract void onShareListReceived(String linkFormat, String[] shares);

    /**
     * Creates the packet.
     *
     * @param ctx      Android application context.
     * @param session  The session for which location is being updated.
     * @param location The updated location data obtained from GNSS/network sensors.
     */
    public LocationUpdatePacket(Context ctx, Session session, Location location) {
        super(ctx, session.getServerURL(), "api/post.php");
        addParameter("lat", String.valueOf(location.getLatitude()));
        addParameter("lon", String.valueOf(location.getLongitude()));
        addParameter("time", String.valueOf((double) System.currentTimeMillis() / 1000D));
        addParameter("sid", session.getID());

        // Not all devices provide these parameters.
        if (location.hasSpeed()) addParameter("spd", String.valueOf(location.getSpeed()));
        if (location.hasAccuracy()) addParameter("acc", String.valueOf(location.getAccuracy()));
    }

    @Override
    protected final void onSuccess(String[] data, Version backendVersion) throws ServerException {
        // Somehow the data array can be empty? Check for this.
        if (data.length < 1) {
            throw new ServerException(getContext(), R.string.err_empty);
        }

        if (data[0].equals("OK")) {
            // If the backend is >= v1.2, post.php returns a list of currently active share links.
            // Update the user interface to include these.
            if (backendVersion.atLeast(HaukConst.VERSION_COMPAT_VIEW_ID)) {

                // The share link list is comma-separated.
                String linkFormat = data[1];
                String shareCSV = data[2];
                if (shareCSV.length() > 0) {
                    onShareListReceived(linkFormat, shareCSV.split(","));
                } else {
                    onShareListReceived(linkFormat, new String[0]);
                }
            }
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
