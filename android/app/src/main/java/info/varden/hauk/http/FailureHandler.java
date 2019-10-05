package info.varden.hauk.http;

/**
 * A base interface for interfaces that can throw a failure state.
 *
 * @author Marius Lindvall
 */
public interface FailureHandler {
    /**
     * Called if a failure occurred in the previous request.
     *
     * @param ex The exception that was thrown from the request.
     */
    void onFailure(Exception ex);
}
