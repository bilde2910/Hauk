package info.varden.hauk.http;

import android.content.Context;
import android.location.Location;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Version;
import info.varden.hauk.utils.TimeUtils;

/**
 * Packet that is sent to update the client's location on the map.
 *
 * @author Marius Lindvall
 */
public abstract class LocationUpdatePacket extends Packet {
    /**
     * Called whenever a list of currently active shares are received from the server. This list may
     * be updated by the server if the user is adopted into a group share. This function is called
     * so that the UI can be updated with these additional group shares, making the user aware that
     * they have been adopted and are now part of a new group.
     *
     * @since 1.2
     * @param linkFormat A string that can be used to construct a public view link for each share
     *                   in the {@code shares} array through {@code String.format(linkFormat,
     *                   shareID)}.
     * @param shares     A list of share IDs active for the user's current session on the backend.
     */
    protected abstract void onShareListReceived(String linkFormat, String[] shares);

    /**
     * Creates the packet.
     *
     * @param ctx      Android application context.
     * @param session  The session for which location is being updated.
     * @param location The updated location data obtained from GNSS/network sensors.
     */
    public LocationUpdatePacket(Context ctx, Session session, Location location) {
        super(ctx, session.getServerURL(), Constants.URL_PATH_POST_LOCATION);
        setParameter(Constants.PACKET_PARAM_LATITUDE, String.valueOf(location.getLatitude()));
        setParameter(Constants.PACKET_PARAM_LONGITUDE, String.valueOf(location.getLongitude()));
        setParameter(Constants.PACKET_PARAM_TIMESTAMP, String.valueOf(System.currentTimeMillis() / (double) TimeUtils.MILLIS_PER_SECOND));
        setParameter(Constants.PACKET_PARAM_SESSION_ID, session.getID());

        // Not all devices provide these parameters.
        if (location.hasSpeed()) setParameter(Constants.PACKET_PARAM_SPEED, String.valueOf(location.getSpeed()));
        if (location.hasAccuracy()) setParameter(Constants.PACKET_PARAM_ACCURACY, String.valueOf(location.getAccuracy()));
    }

    @Override
    protected final void onSuccess(String[] data, Version backendVersion) throws ServerException {
        // Somehow the data array can be empty? Check for this.
        if (data.length < 1) {
            throw new ServerException(getContext(), R.string.err_empty);
        }

        if (data[0].equals(Constants.PACKET_RESPONSE_OK)) {
            // If the backend is >= v1.2, post.php returns a list of currently active share links.
            // Update the user interface to include these.
            if (backendVersion.isAtLeast(Constants.VERSION_COMPAT_VIEW_ID)) {

                // The share link list is comma-separated.
                String linkFormat = data[1];
                String shareCSV = data[2];
                if (!shareCSV.isEmpty()) {
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
                err.append(System.lineSeparator());
            }
            throw new ServerException(err.toString());
        }
    }
}
