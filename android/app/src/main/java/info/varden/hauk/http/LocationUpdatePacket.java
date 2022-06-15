package info.varden.hauk.http;

import android.content.Context;
import android.location.Location;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.http.parameter.LocationProvider;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Version;
import info.varden.hauk.utils.Log;
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
    protected LocationUpdatePacket(Context ctx, Session session, Location location, LocationProvider accuracy) {
        super(ctx, session.getServerURL(), session.getConnectionParameters(), Constants.URL_PATH_POST_LOCATION);
        setParameter(Constants.PACKET_PARAM_SESSION_ID, session.getID());

        Cipher cipher = initCipher(session);

        encryptAndSetParameter(Constants.PACKET_PARAM_LATITUDE, location.getLatitude(), cipher);
        encryptAndSetParameter(Constants.PACKET_PARAM_LONGITUDE, location.getLongitude(), cipher);
        encryptAndSetParameter(Constants.PACKET_PARAM_PROVIDER_ACCURACY, accuracy.getMode(), cipher);
        encryptAndSetParameter(Constants.PACKET_PARAM_TIMESTAMP, System.currentTimeMillis() / (double) TimeUtils.MILLIS_PER_SECOND, cipher);

        // Not all devices provide these parameters:
        if (location.hasSpeed()) encryptAndSetParameter(Constants.PACKET_PARAM_SPEED, location.getSpeed(), cipher);
        if (location.hasAccuracy()) encryptAndSetParameter(Constants.PACKET_PARAM_ACCURACY, location.getAccuracy(), cipher);
        if (location.hasAltitude()) encryptAndSetParameter(Constants.PACKET_PARAM_ALTITUDE, location.getAltitude(), cipher);
    }

    @SuppressWarnings("DesignForExtension")
    @Override
    protected void onSuccess(String[] data, Version backendVersion) throws ServerException {
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

    private Cipher initCipher(Session session) {
        Cipher cipher = null;
        if (session.getDerivableE2EKey() != null) {
            try {
                cipher = Cipher.getInstance(Constants.E2E_TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, session.getDerivableE2EKey().deriveSpec(), new SecureRandom());
                byte[] iv = cipher.getIV();
                setParameter(Constants.PACKET_PARAM_INIT_VECTOR, Base64.encodeToString(iv, Base64.DEFAULT));
            } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | NoSuchPaddingException exception) {
                Log.e("Error was thrown while initializing E2E encryption", exception); //NON-NLS
            }
        }
        return cipher;
    }

    private <V> void encryptAndSetParameter(String key, V value, Cipher cipher) {
        if (cipher != null) {
            // We're using end-to-end encryption - generate an IV and encrypt all parameters.
            try {
                setParameter(key, Base64.encodeToString(cipher.doFinal(String.valueOf(value).getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT));
            } catch (BadPaddingException | IllegalBlockSizeException exception) {
                Log.e("Error was thrown while encrypting location data", exception); //NON-NLS
            }
        } else {
            // If not using end-to-end encryption, send parameters in plain text.
            setParameter(key, String.valueOf(value));
        }
    }
}
