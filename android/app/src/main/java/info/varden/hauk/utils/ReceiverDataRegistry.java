package info.varden.hauk.utils;

import java.util.HashMap;
import java.util.Random;

/**
 * Receiver classes and services are instantiated by Android itself, and we cannot pass arbitrary
 * objects to them during construction. They receive Intents, where we can put some basic data, but
 * for transferring custom objects, the receivers must retrieve the objects themselves from
 * elsewhere. The purpose of this class is to act as a registry where objects can be placed and
 * substituted with an index, which can be set in the Intent data. The receiver then uses that index
 * to retrieve the object itself from the registry, allowing transfers of complex objects to
 * receiver classes.
 *
 * @author Marius Lindvall
 */
public class ReceiverDataRegistry {
    private static HashMap<Integer, Object> data = new HashMap<>();
    private static Random random = new Random();

    /**
     * Registers the given object in the registry.
     *
     * @param obj The object to register.
     * @return An index which can be used to retrieve the object later using retrieve().
     */
    public static int register(Object obj) {
        int index = random.nextInt();
        data.put(index, obj);
        return index;
    }

    /**
     * Retrieves an object from the registry given its index.
     *
     * @param index The index obtained when registering the object using register().
     * @return The object that was stored in the registry.
     */
    public static Object retrieve(int index) {
        return data.remove(index);
    }
}
