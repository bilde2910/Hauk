package info.varden.hauk.utils;

import android.util.Base64;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Helper class that serializes a serializable class to and from Base64-encoded strings for storage
 * in Android shared preferences.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public enum StringSerializer {
    ;

    /**
     * Serializes an object instance.
     *
     * @param obj The object to serialize.
     *
     * @return A Base64-encoded representation of the object.
     */
    public static String serialize(Serializable obj) {
        @SuppressWarnings("SpellCheckingInspection")
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        } catch (IOException e) {
            Log.e(String.format("Exception thrown when serializing instance of %s", obj.getClass().getName()), e); //NON-NLS
        }
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    /**
     * Deserialize an object instance.
     *
     * @param pref The Base64-encoded representation of the object.
     * @param <T>  The serializable class type to cast to when de-serializing.
     * @return The de-serialized object.
     */
    @Nullable
    public static <T extends Serializable> T deserialize(String pref) {
        if (pref == null) return null;
        T obj = null;
        @SuppressWarnings("SpellCheckingInspection")
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(pref, Base64.DEFAULT));
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            //noinspection unchecked
            obj = (T) ois.readObject();
        } catch (Exception e) {
            Log.e("Exception thrown when de-serializing a serializable instance", e); //NON-NLS
        }
        return obj;
    }
}
