package info.varden.hauk.utils;

import android.content.SharedPreferences;

/**
 * Represents a preference key to default value mapping pair for use with storing preferences for
 * Hauk on the device.
 *
 * @param <T> The type of data to store in the preference.
 * @author Marius Lindvall
 */
public abstract class Preference<T> {

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
     * Represents a String-value preference.
     */
    public static final class String extends Preference<java.lang.String> {
        private final java.lang.String key;
        private final java.lang.String def;

        public String(java.lang.String key, java.lang.String def) {
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

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public java.lang.String toString() {
            return "Preference<String>{key=" + this.key + ",default=" + this.def + "}";
        }
    }

    /**
     * Represents an Integer-value preference.
     */
    public static final class Integer extends Preference<java.lang.Integer> {
        private final java.lang.String key;
        private final int def;

        public Integer(java.lang.String key, int def) {
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

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public java.lang.String toString() {
            return "Preference<Integer>{key=" + this.key + ",default=" + this.def + "}";
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

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public java.lang.String toString() {
            return "Preference<Boolean>{key=" + this.key + ",default=" + this.def + "}";
        }
    }
}
