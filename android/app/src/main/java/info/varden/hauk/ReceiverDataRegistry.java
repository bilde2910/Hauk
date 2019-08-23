package info.varden.hauk;

import java.util.HashMap;
import java.util.Random;

public class ReceiverDataRegistry {
    private static HashMap<Integer, Object> data = new HashMap<>();
    private static Random random = new Random();

    public static int register(Object obj) {
        int index = random.nextInt();
        data.put(index, obj);
        return index;
    }

    public static Object retrieve(int index) {
        return data.remove(index);
    }
}
