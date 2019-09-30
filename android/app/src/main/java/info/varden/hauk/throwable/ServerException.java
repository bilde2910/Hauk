package info.varden.hauk.throwable;

import android.content.Context;

/**
 * An exception thrown when there is an error in the response from the server to any packet sent
 * from the app.
 *
 * @author Marius Lindvall
 */
public class ServerException extends Exception {
    /**
     * Create the exception with a String message.
     * @param message The error message.
     */
    public ServerException(String message) {
        super(message);
    }

    /**
     * Create the exception with a resource reference.
     * @param ctx     Android application context.
     * @param message Reference to the error message in strings.xml.
     */
    public ServerException(Context ctx, int message) {
        this(ctx.getString(message));
    }
}
