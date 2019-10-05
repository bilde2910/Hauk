package info.varden.hauk.system;

/**
 * An exception that is thrown if location services are disabled when attempting to start a sharing
 * session.
 *
 * @author Marius Lindvall
 */
public class LocationServicesDisabledException extends Exception {
    private static final long serialVersionUID = -345262642944634900L;

    public LocationServicesDisabledException() {
        super("Location services are disabled");
    }
}
