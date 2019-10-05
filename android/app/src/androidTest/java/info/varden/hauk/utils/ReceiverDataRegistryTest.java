package info.varden.hauk.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public final class ReceiverDataRegistryTest {

    @Test
    public void registryTest() {
        // Create a dummy object to store
        List<Integer> testObject = new ArrayList<>();
        testObject.add(48);
        testObject.add(19);

        // Store the object
        int index = ReceiverDataRegistry.register(testObject);

        // Retrieve the object
        Object retrieved = ReceiverDataRegistry.retrieve(index);
        assertThat("Retrieved object is wrong type", retrieved, is(instanceOf(testObject.getClass())));
        assertThat("Retrieved object is different instance", retrieved, is(sameInstance((Object) testObject)));
    }
}