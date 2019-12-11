package info.varden.hauk.system.preferences;

/**
 * An exception that is thrown when attempting to read a setting that is not compatible with the
 * given type on the settings screen.
 *
 * @author Marius Lindvall
 */
class InvalidPreferenceTypeException extends RuntimeException {
    private static final long serialVersionUID = -1966346602996946755L;

    InvalidPreferenceTypeException(Object value, Class<?> target) {
        super(String.format("Cannot direct-cast %s to %s", value.toString(), target.getName())); //NON-NLS
    }
}
