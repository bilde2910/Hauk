package info.varden.hauk.utils;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Helper class that serializes a serializable class to and from Base64-encoded strings for storage
 * in Android shared preferences.
 *
 * @param <T> The serializable class type to cast to when deserializing.
 */
public class StringSerializer<T extends Serializable> {
    /**
     * Serializes an object instance.
     *
     * @param obj The object to serialize.
     * @return A Base64-encoded representation of the object.
     */
    public String serialize(T obj) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    /**
     * Deserializes an object instance.
     *
     * @param pref The Base64-encoded representation of the object.
     * @return The deserialized object.
     */
    public T deserialize(String pref) {
        if (pref == null) return null;
        T obj = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(pref, Base64.DEFAULT));
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            obj = (T) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }
}
