package info.varden.hauk.system.security;

import java.io.Serializable;

/**
 * Structure that contains encrypted data along with an initialization vector.
 *
 * @author Marius Lindvall
 */
public final class EncryptedData implements Serializable {
    private static final long serialVersionUID = -6247689274316948477L;

    private final byte[] iv;
    private final byte[] data;

    /**
     * Creates an encrypted data instance.
     *
     * @param iv   An encryption initialization vector.
     * @param data Encrypted binary data.
     */
    EncryptedData(byte[] iv, byte[] data) {
        this.iv = iv.clone();
        this.data = data.clone();
    }

    byte[] getIV() {
        return this.iv.clone();
    }

    byte[] getMessage() {
        return this.data.clone();
    }
}
