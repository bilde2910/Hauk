package info.varden.hauk.system.preferences;

/**
 * An exception that is thrown if assignment ot a value to a preference fails due to a downstream
 * exception.
 *
 * @author Marius Lindvall
 */
class PreferenceAssignmentException extends RuntimeException {
    private static final long serialVersionUID = 8233494694851033874L;

    PreferenceAssignmentException(Exception parent) {
        super(parent);
    }
}
