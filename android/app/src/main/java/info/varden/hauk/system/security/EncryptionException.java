package info.varden.hauk.system.security;

/**
 * A wrapper exception that is thrown if errors happen during encryption or decryption in
 * {@link KeyStoreHelper}.
 *
 * @author Marius Lindvall
 */
public final class EncryptionException extends Exception {
    private static final long serialVersionUID = 1413652344744489876L;

    EncryptionException(Exception ex) {
        super(ex);
    }
}
