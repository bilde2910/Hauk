package info.varden.hauk.utils;

import org.junit.Test;

import info.varden.hauk.struct.Version;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public final class StringSerializerTest {

    @Test
    public void stateEqual() {
        Version testObject = new Version("3.14.159");

        String serialized = StringSerializer.serialize(testObject);
        Version deSerialized = StringSerializer.deserialize(serialized);

        assertThat("Does not serialize", deSerialized, is(not(sameInstance(testObject))));
        assertThat("Does not equal", deSerialized, is(equalTo(testObject)));
    }
}