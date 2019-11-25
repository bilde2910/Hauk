package info.varden.hauk.system.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import info.varden.hauk.utils.Log;

/**
 * Helper class that interacts with the Android key store and offers simple methods for encrypting
 * and decrypting data.
 *
 * @author Marius Lindvall
 */
public final class KeyStoreHelper {
    @SuppressWarnings("HardCodedStringLiteral")
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    @SuppressWarnings("HardCodedStringLiteral")
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_AUTH_TAG_LENGTH = 128;

    private static KeyStore store = null;
    private SecretKey key = null;

    /**
     * Retrieves a key store helper for the given key store alias.
     *
     * @param alias The alias to retrieve a helper for.
     */
    public KeyStoreHelper(KeyStoreAlias alias) {
        try {
            // Load the key store if not already loaded.
            if (store == null) loadKeyStore();

            // Check if the alias exists. If not, create it.
            if (!store.containsAlias(alias.getAlias())) {
                Log.i("Generating new key for alias %s", alias); //NON-NLS
                KeyGenerator keygen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(alias.getAlias(), KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build();
                keygen.init(spec);
                this.key = keygen.generateKey();
            } else {
                Log.i("Loading existing key for alias %s", alias); //NON-NLS
                KeyStore.SecretKeyEntry keyEntry = (KeyStore.SecretKeyEntry) store.getEntry(alias.getAlias(), null);
                this.key = keyEntry.getSecretKey();
            }
        } catch (Exception e) {
            Log.e("Unable to load key store or generate keys", e); //NON-NLS
        }
    }

    /**
     * Encrypts the given data.
     *
     * @param data The data to encrypt.
     * @return The encrypted data and IV.
     * @throws EncryptionException if there was an error while encrypting.
     */
    private EncryptedData encrypt(byte[] data) throws EncryptionException {
        Log.v("Encrypting data"); //NON-NLS

        // Catch errors during initialization.
        if (this.key == null) throw new EncryptionException(new InvalidKeyException("Encryption key is null"));

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, this.key);
            byte[] iv = cipher.getIV();
            byte[] message = cipher.doFinal(data);
            return new EncryptedData(iv, message);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    /**
     * Decrypts the given data.
     *
     * @param data The data to decrypt.
     * @return The cleartext data.
     * @throws EncryptionException if there was an error while decrypting.
     */
    private byte[] decrypt(EncryptedData data) throws EncryptionException {
        Log.v("Decrypting data"); //NON-NLS

        // Catch errors during initialization.
        if (this.key == null) throw new EncryptionException(new InvalidKeyException("Decryption key is null"));

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, data.getIV());
            cipher.init(Cipher.DECRYPT_MODE, this.key, spec);
            return cipher.doFinal(data.getMessage());
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    /**
     * Encrypts the given string.
     *
     * @param data The string to encrypt.
     * @return The encrypted data and IV.
     * @throws EncryptionException if there was an error while encrypting.
     */
    public EncryptedData encryptString(String data) throws EncryptionException {
        return encrypt(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decrypts the given string.
     *
     * @param data The string to decrypt.
     * @return The cleartext string.
     * @throws EncryptionException if there was an error while decrypting.
     */
    public String decryptString(EncryptedData data) throws EncryptionException {
        return new String(decrypt(data), StandardCharsets.UTF_8);
    }

    /**
     * Loads the Android key store.
     *
     * @throws Exception if the loading failed.
     */
    private static void loadKeyStore() throws Exception {
        store = KeyStore.getInstance(ANDROID_KEY_STORE);
        store.load(null);
    }
}
