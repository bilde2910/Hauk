package info.varden.hauk.struct;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import info.varden.hauk.HaukConst;
import info.varden.hauk.utils.TimeUtils;

/**
 * A data structure that contains all data required to maintain a session against a Hauk server.
 *
 * @author Marius Lindvall
 */
public class Session implements Serializable {
    private final String serverURL;
    private final Version backendVersion;
    private final String sessionID;
    private final long expiry;
    private final int interval;

    public Session(String serverURL, Version backendVersion, String sessionID, long expiry, int interval) {
        this.serverURL = serverURL;
        this.backendVersion = backendVersion;
        this.sessionID = sessionID;
        this.expiry = expiry;
        this.interval = interval;
    }

    public String getServerURL() {
        return this.serverURL;
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

    @SuppressWarnings("WeakerAccess")
    public Date getExpiryDate() {
        return new Date(getExpiryTime());
    }

    public String getExpiryString() {
        SimpleDateFormat formatter = new SimpleDateFormat(HaukConst.DATE_FORMAT, Locale.getDefault());
        return formatter.format(getExpiryDate());
    }

    public boolean isActive() {
        return System.currentTimeMillis() < getExpiryTime();
    }

    public int getRemainingSeconds() {
        return (int) (getRemainingMillis() / (long) TimeUtils.MILLIS_PER_SECOND);
    }

    public long getRemainingMillis() {
        return getExpiryTime() - System.currentTimeMillis();
    }

    @SuppressWarnings("WeakerAccess")
    public int getIntervalSeconds() {
        return this.interval;
    }

    public long getIntervalMillis() {
        return (long) getIntervalSeconds() * (long) TimeUtils.MILLIS_PER_SECOND;
    }
}
