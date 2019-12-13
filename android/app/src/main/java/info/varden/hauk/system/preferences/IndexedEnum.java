package info.varden.hauk.system.preferences;

import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * A base class for enum-like values that can be stored in preferences. A class can extend this
 * class to allow it to be stored as an integer in preferences and be retrieved directly using
 * {@link PreferenceManager#get(Preference)}.
 *
 * @param <T> The type that extends this class.
 */
public abstract class IndexedEnum<T extends IndexedEnum<T>> implements Serializable {
    private static final long serialVersionUID = -1867612075461184507L;

    /**
     * An internal ID for this enum member that represents the value it is stored as in preferences.
     */
    private final int index;

    protected IndexedEnum(int index) {
        this.index = index;
    }

    @SuppressWarnings("MethodOverloadsMethodOfSuperclass")
    public final boolean equals(IndexedEnum<T> other) {
        return this.getIndex() == other.getIndex();
    }

    public final int getIndex() {
        return this.index;
    }

    /**
     * Returns an enum member by its index.
     *
     * @param index The index of the enum member.
     * @return An enum member.
     */
    @SuppressWarnings("unchecked")
    final T fromIndex(int index) throws IllegalAccessException, InstantiationException {
        Field[] fields = getClass().getFields();
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(getClass())) {
                IndexedEnum<T> instance = (IndexedEnum<T>) field.get(null);
                if (instance != null && instance.getIndex() == index) {
                    return (T) instance;
                }
            }
        }
        throw new InstantiationException("Failed to find a member with this index");
    }

    @SuppressWarnings("DesignForExtension")
    @Override
    public String toString() {
        return "index=" + this.index;
    }
}
