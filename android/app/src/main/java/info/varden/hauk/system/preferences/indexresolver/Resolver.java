package info.varden.hauk.system.preferences.indexresolver;

import java.io.Serializable;

import info.varden.hauk.system.preferences.IndexedEnum;

/**
 * A class that provides mappings between an index and a specific type of object for easy
 * translation when reading preferences.
 *
 * @param <T1> The type of class implementing this class.
 * @param <T2> The class that each entry in the parent class should map to.
 */
public abstract class Resolver<T1 extends IndexedEnum<T1>, T2 extends Serializable> extends IndexedEnum<T1> {
    private static final long serialVersionUID = 1829235445367254385L;

    private final T2 mapping;

    protected Resolver(int index, T2 mapping) {
        super(index);
        this.mapping = mapping;
    }

    public final T2 resolve() {
        return this.mapping;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "{mapping=" + this.mapping + "," + super.toString() + "}";
    }
}
