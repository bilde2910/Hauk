package info.varden.hauk.http;

import android.content.Context;

import java.util.HashMap;

import info.varden.hauk.struct.Version;
import info.varden.hauk.throwable.ServerException;

/**
 * Base class for all communication packets used to send data to the server. Should be extended by
 * other classes in the same package as this one to implement appropriate functionality and to not
 * bloat other classes with request code.
 *
 * @author Marius Lindvall
 */
public abstract class Packet {
    private final HashMap<String, String> params;
    private final Context ctx;
    private final String server;
    private final String path;

    /**
     * Called if the request is successful.
     *
     * @param data           An array of strings received from the server, where each string
     *                       represents a line of received data.
     * @param backendVersion The version of the backend.
     *
     * @throws ServerException If package packet handling/parsing fails. When thrown, the
     *                         onFailure() function is called with the thrown exception as its
     *                         argument.
     */
    protected abstract void onSuccess(String[] data, Version backendVersion) throws ServerException;

    /**
     * Called if the request failed, or if parsing failed (i.e. exception thrown from onSuccess()).
     *
     * @param ex The exception that was thrown.
     */
    protected abstract void onFailure(Exception ex);

    /**
     * Base contructor for a packet.
     *
     * @param ctx    Android application context.
     * @param server The full Hauk server base URL, including trailing slash.
     * @param path   The path underneath the base URL that should be called.
     */
    protected Packet(Context ctx, String server, String path) {
        this.params = new HashMap<>();
        this.ctx = ctx;
        this.server = server;
        this.path = path;
    }

    /**
     * Adds a parameter to send in the packet data.
     *
     * @param key   The parameter key.
     * @param value The parameter value.
     */
    protected void addParameter(String key, String value) {
        this.params.put(key, value);
    }

    /**
     * Returns Android application context for usage in e.g. creating ServerExceptions.
     */
    protected Context getContext() {
        return this.ctx;
    }

    /**
     * Sends the packet.
     */
    public final void send() {
        HTTPThread req = new HTTPThread(new HTTPThread.Callback() {
            @Override
            public void run(HTTPThread.Response resp) {
                // An exception may have occurred, but it cannot be thrown because this is a
                // callback. Instead, the exception (if any) is stored in the response object.
                Exception e = resp.getException();
                if (e == null) {
                    try {
                        onSuccess(resp.getData(), resp.getServerVersion());
                    } catch (Exception ex) {
                        onFailure(ex);
                    }
                } else {
                    onFailure(e);
                }
            }
        });
        req.execute(new HTTPThread.Request(this.server + this.path, this.params));
    }
}
