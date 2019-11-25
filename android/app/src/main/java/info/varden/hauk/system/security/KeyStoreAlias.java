package info.varden.hauk.system.security;

/**
 * A list of pre-defined encryption aliases for use in encrypting data with {@link KeyStoreHelper}.
 *
 * @author Marius Lindvall
 */
public enum KeyStoreAlias {
    /**
     * Key store alias for use in encrypting and decrypting shared preferences.
     */
    @SuppressWarnings("HardCodedStringLiteral")
    PREFERENCES("sharedPrefs");

    /**
     * The alias of the key in the key store.
     */
    private final String alias;

    KeyStoreAlias(String alias) {
        this.alias = alias;
    }

    String getAlias() {
        return this.alias;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return this.alias;
    }
}
