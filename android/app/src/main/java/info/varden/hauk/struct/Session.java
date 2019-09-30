package info.varden.hauk.struct;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import info.varden.hauk.HaukConst;

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

    public long getExpiryTime() {
        return this.expiry;
    }

    public Date getExpiryDate() {
        return new Date(getExpiryTime());
    }

    public String getExpiryString() {
        return getExpiryString(HaukConst.DATE_FORMAT);
    }

    public String getExpiryString(String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.getDefault());
        return formatter.format(getExpiryDate());
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() >= getExpiryTime();
    }

    public int getRemainingSeconds() {
        return (int) (getRemainingMillis() / 1000L);
    }

    public long getRemainingMillis() {
        return getExpiryTime() - System.currentTimeMillis();
    }

    public int getIntervalSeconds() {
        return this.interval;
    }

    public long getIntervalMillis() {
        return (long) getIntervalSeconds() * 1000L;
    }
}
