package info.varden.hauk.manager;

import info.varden.hauk.struct.Share;

/**
 * Callback interface that {@link SessionManager} handlers can attach to receive status updates
 * about share attachment.
 *
 * @author Marius Lindvall
 */
public interface ShareListener {
    /**
     * Called when a share has been joined on the server or locally.
     *
     * @param share The share that was joined.
     */
    void onShareJoined(Share share);

    /**
     * Called when a share has been left.
     *
     * @param share The share that was left.
     */
    void onShareParted(Share share);
}
