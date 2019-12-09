package info.varden.hauk.http;

import android.content.Context;

/**
 * An exception thrown when there is an error in the response from the server to any packet sent
 * from the app.
 *
 * @author Marius Lindvall
 */
public class ServerException extends Exception {
    private static final long serialVersionUID = 2879124634145201633L;

    /**
     * Create the exception with a String message.
     *
     * @param message The error message.
     */
    ServerException(String message) {
        super(message);
    }

    /**
     * Create the exception with a resource reference.
     *
     * @param ctx     Android application context.
     * @param message Reference to the error message in strings.xml.
     */
    ServerException(Context ctx, @SuppressWarnings("SameParameterValue") int message) {
        this(ctx.getString(message));
    }
}
