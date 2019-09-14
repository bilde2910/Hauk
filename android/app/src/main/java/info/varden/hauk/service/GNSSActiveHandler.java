package info.varden.hauk.service;

/**
 * Interface template for handling UI updates when location data is received.
 *
 * @author Marius Lindvall
 */
public interface GNSSActiveHandler {
    /**
     * Called when the initial low-accuracy GNSS fix has been obtained.
     */
    void onCoarseLocationReceived();

    /**
     * Called when the initial high-accuracy GNSS fix has been obtained.
     */
    void onAccurateLocationReceived();
}
