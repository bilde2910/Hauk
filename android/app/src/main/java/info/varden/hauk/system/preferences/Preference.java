package info.varden.hauk.system.preferences;

import android.content.SharedPreferences;

import info.varden.hauk.system.security.EncryptedData;
import info.varden.hauk.system.security.EncryptionException;
import info.varden.hauk.system.security.KeyStoreAlias;
import info.varden.hauk.system.security.KeyStoreHelper;
import info.varden.hauk.utils.Log;
import info.varden.hauk.utils.StringSerializer;

/**
 * Represents a preference key to default value mapping pair for use with storing preferences for
 * Hauk on the device.
 *
 * @param <T> The type of data to store in the preference.
 * @author Marius Lindvall
 */
public abstract class Preference<T> {

    private final java.lang.String key;
    private final T def;
    private final Class<?> type;

    private Preference(java.lang.String key, T def, Class<?> type) {
        this.key = key;
        this.def = def;
        this.type = type;
    }

    /**
     * Returns the key of this preference in the {@link SharedPreferences} instance.
     */
    public final java.lang.String getKey() {
        return this.key;
    }

    public final T getDefault() {
        return this.def;
    }

    /**
     * Returns the type of data this preference stores.
     */
    public final Class<?> getPreferenceType() {
        return this.type;
    }

    /**
     * Gets the value of the preference from the given preference object.
     *
     * @param prefs The shared preferences to retrieve the value from.
     */
    abstract T get(SharedPreferences prefs);

    /**
     * Sets the value of the preference to the given value in the preference object.
     *
     * @param prefs The shared preferences to write the value to.
     * @param value The value to write.
     */
    abstract void set(SharedPreferences.Editor prefs, T value);

    /**
     * Checks whether or not the preference exists in the given preference object.
     *
     * @param prefs The shared preferences to check for preference existence in.
     */
    public final boolean has(SharedPreferences prefs) {
        return prefs.contains(this.key);
    }

    /**
     * Clears the preference from the given preference object.
     *
     * @param prefs The shared preferences to clear the value from.
     */
    public final void clear(SharedPreferences.Editor prefs) {
        prefs.remove(this.key);
    }

    /**
     * Returns whether or not this preference is expected to contain sensitive information that
     * should not be logged.
     */
    abstract boolean isSensitive();

    /**
     * Represents a String-value preference.
     */
    public static final class String extends Preference<java.lang.String> {
        private final java.lang.String key;
        private final java.lang.String def;

        public String(java.lang.String key, java.lang.String def) {
            super(key, def, java.lang.String.class);
            this.key = key;
            this.def = def;
        }

        @Override
        java.lang.String get(SharedPreferences prefs) {
            return prefs.getString(this.key, this.def);
        }

        @Override
        void set(SharedPreferences.Editor prefs, java.lang.String value) {
            prefs.putString(this.key, value);
        }

        @Override
        boolean isSensitive() {
            return false;
        }

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public java.lang.String toString() {
            return "Preference<String>{key=" + this.key + ",default=" + this.def + "}";
        }
    }

    /**
     * Represents an encrypted String-value preference.
     */
    public static final class EncryptedString extends Preference<java.lang.String> {
        private final java.lang.String key;
        private final java.lang.String def;

        public EncryptedString(java.lang.String key, java.lang.String def) {
            super(key, def, java.lang.String.class);
            this.key = key;
            this.def = def;
        }

        @Override
        java.lang.String get(SharedPreferences prefs) {
            if (!has(prefs)) return this.def;
            EncryptedData data = StringSerializer.deserialize(prefs.getString(this.key, null));
            try {
                return new KeyStoreHelper(KeyStoreAlias.PREFERENCES).decryptString(data);
            } catch (EncryptionException ex) {
                Log.e("Failed to retrieve preference %s due to a decryption error", ex, this.key); //NON-NLS
                return this.def;
            }
        }

        @Override
        void set(SharedPreferences.Editor prefs, java.lang.String value) {
            try {
                EncryptedData data = new KeyStoreHelper(KeyStoreAlias.PREFERENCES).encryptString(value);
                prefs.putString(this.key, StringSerializer.serialize(data));
            } catch (EncryptionException ex) {
                Log.e("Failed to store preference %s due to an encryption error", ex, this.key); //NON-NLS
            }
        }

        @Override
        boolean isSensitive() {
            return true;
        }

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public java.lang.String toString() {
            return "Preference<String+Encrypted>{key=" + this.key + ",default=" + this.def + "}";
        }
    }

    /**
     * Represents an Integer-value preference.
     */
    public static final class Integer extends Preference<java.lang.Integer> {
        private final java.lang.String key;
        private final int def;

        public Integer(java.lang.String key, int def) {
            super(key, def, java.lang.Integer.class);
            this.key = key;
            this.def = def;
        }

        @Override
        java.lang.Integer get(SharedPreferences prefs) {
            return prefs.getInt(this.key, this.def);
        }

        @Override
        void set(SharedPreferences.Editor prefs, java.lang.Integer value) {
            prefs.putInt(this.key, value);
        }

        @Override
        boolean isSensitive() {
            return false;
        }

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public java.lang.String toString() {
            return "Preference<Integer>{key=" + this.key + ",default=" + this.def + "}";
        }
    }

    public static final class Enum<U extends IndexedEnum<U>> extends Preference<U> {
        private final java.lang.String key;
        private final U def;

        public Enum(java.lang.String key, U def) {
            super(key, def, IndexedEnum.class);
            this.key = key;
            this.def = def;
        }

        @Override
        U get(SharedPreferences prefs) {
            try {
                return this.def.fromIndex(prefs.getInt(this.key, this.def.getIndex()));
            } catch (Exception e) {
                return this.def;
            }
        }

        @Override
        void set(SharedPreferences.Editor prefs, IndexedEnum value) {
            prefs.putInt(this.key, value.getIndex());
        }

        @Override
        boolean isSensitive() {
            return false;
        }

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public java.lang.String toString() {
            return "Preference<Enum>{key=" + this.key + ",default=" + this.def + "}";
        }
    }

    /**
     * Represents a Boolean-value preference.
     */
    public static final class Boolean extends Preference<java.lang.Boolean> {
        private final java.lang.String key;
        private final boolean def;

        @SuppressWarnings("BooleanParameter")
        public Boolean(java.lang.String key, boolean def) {
            super(key, def, java.lang.Boolean.class);
            this.key = key;
            this.def = def;
        }

        @Override
        java.lang.Boolean get(SharedPreferences prefs) {
            return prefs.getBoolean(this.key, this.def);
        }

        @Override
        void set(SharedPreferences.Editor prefs, java.lang.Boolean value) {
            prefs.putBoolean(this.key, value);
        }

        @Override
        boolean isSensitive() {
            return false;
        }

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public java.lang.String toString() {
            return "Preference<Boolean>{key=" + this.key + ",default=" + this.def + "}";
        }
    }
}
