package info.varden.hauk.manager;

/**
 * A listener interface for clients that want to receive state updates from the GNSS service for
 * active sessions.
 *
 * @author Marius Lindvall
 */
public interface GNSSStatusUpdateListener {
    /**
     * Called when the session and its corresponding GNSS listeners are stopped.
     */
    void onShutdown();

    /**
     * Called when the session is created and when the GNSS listeners have been spawned and bound.
     */
    void onStarted();

    /**
     * <p>Called on first reception of low-accuracy location data from the network. Should be
     * available almost instantly if the user device has network-based or other non-GNSS location
     * sources available and enabled when the session is started.</p>
     *
     * <p>Note that this method will not be called if fine location data is received first.</p>
     */
    void onCoarseLocationReceived();

    /**
     * Called on first reception of high-accuracy location data from GNSS. This may take a while to
     * be available, or may not be available at all if the user is in a location that does not have
     * adequate GNSS signal reception.
     */
    void onAccurateLocationReceived();
}
