package info.varden.hauk.struct;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import info.varden.hauk.Constants;
import info.varden.hauk.utils.StringUtils;

/**
 * Serializable key spec that stores a password and salt for deriving a secret AES key spec.
 *
 * @author Marius Lindvall
 */
public final class KeyDerivable implements Serializable {
    private static final long serialVersionUID = -4298542521894801298L;

    /**
     * Salt used in PBKDF2 for key derivation.
     */
    private final byte[] salt;

    /**
     * End-to-end password to encrypt outgoing data with.
     */
    private final String password;

    /**
     * Secret key spec cache to improve performance for key derivation.
     */
    @SuppressWarnings("FieldNotUsedInToString")
    private transient SecretKeySpec keySpec = null;

    public KeyDerivable(String password, byte[] salt) {
        this.password = password;
        this.salt = salt.clone();
    }

    /**
     * Derives a key spec from this derivable key.
     *
     * @return A secret key spec for use with encryption functions.
     * @throws InvalidKeySpecException if the key spec doesn't exist.
     * @throws NoSuchAlgorithmException if the algorithm doesn't exist.
     */
    public SecretKeySpec deriveSpec() throws InvalidKeySpecException, NoSuchAlgorithmException {
        if (this.keySpec == null) {
            // E2E encryption is used, but the key spec hasn't been cached yet. Generate and cache
            // it, then return the spec.
            KeySpec ks = new PBEKeySpec(this.password.toCharArray(), this.salt, Constants.E2E_PBKDF2_ITERATIONS, Constants.E2E_AES_KEY_SIZE);
            SecretKeyFactory kf = SecretKeyFactory.getInstance(Constants.E2E_KD_FUNCTION);
            byte[] key = kf.generateSecret(ks).getEncoded();
            this.keySpec = new SecretKeySpec(key, Constants.E2E_KEY_SPEC);
        }
        return this.keySpec;
    }

    @Override
    public String toString() {
        return "KeyDerivable{password=" + this.password
                + ",salt=0x" + StringUtils.bytesToHex(this.salt)
                + "}";
    }
}
