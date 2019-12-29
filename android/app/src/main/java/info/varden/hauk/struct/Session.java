package info.varden.hauk.struct;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import info.varden.hauk.Constants;
import info.varden.hauk.http.ConnectionParameters;
import info.varden.hauk.utils.TimeUtils;

/**
 * A data structure that contains all data required to maintain a session against a Hauk server.
 *
 * @author Marius Lindvall
 */
public final class Session implements Serializable {
    private static final long serialVersionUID = 315568255735934584L;

    /**
     * The Hauk backend server base URL.
     */
    private final String serverURL;

    /**
     * Connection parameters for the backend connection.
     */
    private final ConnectionParameters connParams;

    /**
     * The version the backend is running.
     */
    private final Version backendVersion;

    /**
     * A unique session ID provided by the backend to identify the session in all further
     * correspondence after session initiation. In practice, this acts as a temporary password that
     * is generated for each session and is only valid for the duration of the session.
     */
    private final String sessionID;

    /**
     * A timestamp of when the share expires, in milliseconds since the Unix epoch.
     */
    private final long expiry;

    /**
     * The interval between each location update, in seconds.
     */
    private final int interval;

    /**
     * The minimum distance between each location update, in meters.
     */
    private final float minDistance;

    /**
     * End-to-end encryption parameters.
     */
    @Nullable
    private final KeyDerivable e2eParams;

    public Session(String serverURL, ConnectionParameters connParams, Version backendVersion, String sessionID, long expiry, int interval, float minDistance, @Nullable KeyDerivable e2eParams) {
        this.serverURL = serverURL;
        this.backendVersion = backendVersion;
        this.sessionID = sessionID;
        this.expiry = expiry;
        this.interval = interval;
        this.minDistance = minDistance;
        this.e2eParams = e2eParams;
        this.connParams = connParams;
    }

    @Override
    public String toString() {
        return "Session{serverURL=" + this.serverURL
                + ",connParams=" + this.connParams
                + ",backendVersion=" + this.backendVersion
                + ",sessionID=" + this.sessionID
                + ",expiry=" + this.expiry
                + ",interval=" + this.interval
                + ",e2eParams=" + this.e2eParams
                + "}";
    }

    public String getServerURL() {
        return this.serverURL;
    }

    public ConnectionParameters getConnectionParameters() {
        return this.connParams;
    }

    public Version getBackendVersion() {
        return this.backendVersion;
    }

    public String getID() {
        return this.sessionID;
    }

    @SuppressWarnings("WeakerAccess")
    public long getExpiryTime() {
        return this.expiry;
    }

    /**
     * Returns the expiration time of this session as a {@link Date} object.
     */
    @SuppressWarnings("WeakerAccess")
    public Date getExpiryDate() {
        return new Date(getExpiryTime());
    }

    /**
     * Returns the expiration time of this session as a human-readable string.
     */
    public String getExpiryString() {
        SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT_UI, Locale.getDefault());
        return formatter.format(getExpiryDate());
    }

    /**
     * Whether or not this share is still active and has not expired.
     */
    public boolean isActive() {
        return System.currentTimeMillis() < getExpiryTime();
    }

    /**
     * Returns the number of seconds remaining of the location share.
     */
    public long getRemainingSeconds() {
        return getRemainingMillis() / TimeUtils.MILLIS_PER_SECOND;
    }

    /**
     * Returns the number of milliseconds remaining of the location share.
     */
    public long getRemainingMillis() {
        return getExpiryTime() - System.currentTimeMillis();
    }

    /**
     * Returns the interval between each location update, in seconds.
     */
    @SuppressWarnings("WeakerAccess")
    public int getIntervalSeconds() {
        return this.interval;
    }

    /**
     * Returns the interval between each location update, in milliseconds.
     */
    public long getIntervalMillis() {
        return getIntervalSeconds() * TimeUtils.MILLIS_PER_SECOND;
    }

    /**
     * Returns the minimum distance between each location update, in meters.
     */
    public float getMinimumDistance() {
        return this.minDistance;
    }

    @Nullable
    public KeyDerivable getDerivableE2EKey() {
        return this.e2eParams;
    }
}
