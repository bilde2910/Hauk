package info.varden.hauk.system;

/**
 * An exception that is thrown when a sharing session is started, but location permissions have not
 * been granted yet.
 *
 * @author Marius Lindvall
 */
public class LocationPermissionsNotGrantedException extends Exception {
    private static final long serialVersionUID = -5852712724878248514L;

    public LocationPermissionsNotGrantedException() {
        super("Location permissions have not been granted");
    }
}
