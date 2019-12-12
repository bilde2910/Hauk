package info.varden.hauk.system.preferences;

import android.content.Context;

import androidx.preference.PreferenceDataStore;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import info.varden.hauk.Constants;
import info.varden.hauk.utils.Log;

/**
 * Preference interceptor data store that redirects preference storage requests to
 * {@link PreferenceManager} and {@link Preference} for proper validation and storage. This allows
 * preferences to be encrypted, for example.
 *
 * @author Marius Lindvall
 */
public final class PreferenceHandler extends PreferenceDataStore {
    /**
     * Mapping between all preference keys and {@link Preference}s.
     */
    private static final Map<String, Preference> map;

    static {
        // Initialize the preference map.
        map = new HashMap<>();

        // Find all Preferences declared in the Constants class and add them to the map.
        Field[] fields = Constants.class.getFields();
        for (Field f : fields) {
            if (f.getType().isAssignableFrom(Preference.class)) {
                try {
                    Log.v("Found field %s of type Preference in Constants, adding to map", f.getName()); //NON-NLS
                    Preference p = (Preference) f.get(null);
                    map.put(p.getKey(), p);
                } catch (IllegalAccessException e) {
                    Log.wtf("Failed to read constant from Constants", e); //NON-NLS
                }
            }
        }
    }

    /**
     * Hauk preference manager.
     */
    private final PreferenceManager manager;

    public PreferenceHandler(Context ctx) {
        this.manager = new PreferenceManager(ctx);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Log.v("Getting boolean key %s", key); //NON-NLS
        if (!map.containsKey(key)) throw new PreferenceNotFoundException(key);
        Object value = this.manager.get(map.get(key));
        if (value instanceof Boolean) {
            return (boolean) value;
        } else {
            throw new InvalidPreferenceTypeException(value, Boolean.class);
        }
    }

    @Override
    public float getFloat(String key, float defValue) {
        Log.v("Getting float key %s", key); //NON-NLS
        if (!map.containsKey(key)) throw new PreferenceNotFoundException(key);
        Object value = this.manager.get(map.get(key));
        if (value instanceof Float) {
            return (float) value;
        } else {
            throw new InvalidPreferenceTypeException(value, Float.class);
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        Log.v("Getting int key %s", key); //NON-NLS
        if (!map.containsKey(key)) throw new PreferenceNotFoundException(key);
        Object value = this.manager.get(map.get(key));
        if (value instanceof Integer) {
            return (int) value;
        } else {
            throw new InvalidPreferenceTypeException(value, Integer.class);
        }
    }

    @Override
    public long getLong(String key, long defValue) {
        Log.v("Getting long key %s", key); //NON-NLS
        if (!map.containsKey(key)) throw new PreferenceNotFoundException(key);
        Object value = this.manager.get(map.get(key));
        if (value instanceof Long) {
            return (long) value;
        } else {
            throw new InvalidPreferenceTypeException(value, Long.class);
        }
    }

    @Override
    public String getString(String key, String defValue) {
        Log.v("Getting string key %s", key); //NON-NLS
        if (!map.containsKey(key)) throw new PreferenceNotFoundException(key);
        Object value = this.manager.get(map.get(key));
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Integer || value instanceof Float || value instanceof Long) {
            // EditTextPreference calls getString() instead of getInt(), getFloat() and getLong()
            // because it is a text input field, despite the type of data it is set to store. This
            // must be handled properly.
            return String.valueOf(value);
        } else {
            throw new InvalidPreferenceTypeException(value, String.class);
        }
    }

    @Override
    public void putBoolean(String key, boolean value) {
        this.manager.set(map.get(key), value);
    }

    @Override
    public void putFloat(String key, float value) {
        this.manager.set(map.get(key), value);
    }

    @Override
    public void putInt(String key, int value) {
        this.manager.set(map.get(key), value);
    }

    @Override
    public void putLong(String key, long value) {
        this.manager.set(map.get(key), value);
    }

    @Override
    public void putString(String key, String value) {
        // EditTextPreferences calls putString() instead of putInt(), putFloat() and putLong()
        // because it is a text input field, despite the type of data it is set to store. This
        // must be handled properly.
        Class<?> type = map.get(key).getPreferenceType();
        if (type == Integer.class) {
            putInt(key, Integer.valueOf(value));
        } else if (type == Float.class) {
            putFloat(key, Float.valueOf(value));
        } else if (type == Long.class) {
            putLong(key, Long.valueOf(value));
        } else {
            this.manager.set(map.get(key), value);
        }
    }
}
