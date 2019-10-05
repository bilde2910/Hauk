package info.varden.hauk.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@SuppressWarnings({"DuplicateStringLiteralInspection", "HardCodedStringLiteral"})
public final class PreferenceTest {

    private static final String PREFERENCE_KEY_STRING = "DummyKeyString";
    private static final String PREFERENCE_KEY_INT = "DummyKeyInt";
    private static final String PREFERENCE_KEY_BOOL = "DummyKeyBool";

    private SharedPreferences prefs;

    private Preference<String> testPrefString;
    private Preference<Integer> testPrefInt;
    private Preference<Boolean> testPrefBool;

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        this.prefs = ctx.getSharedPreferences("TestPreferences", Context.MODE_PRIVATE);

        this.testPrefString = new Preference.String(PREFERENCE_KEY_STRING, "HelloWorld");
        this.testPrefInt = new Preference.Integer(PREFERENCE_KEY_INT, 17);
        this.testPrefBool = new Preference.Boolean(PREFERENCE_KEY_BOOL, true);
    }

    @Test
    public void retrieveDefault() {
        this.prefs.edit().clear().apply();
        assertThat("Default string not returned", this.testPrefString.get(this.prefs), is("HelloWorld"));
        assertThat("Default integer not returned", this.testPrefInt.get(this.prefs), is(17));
        assertThat("Default boolean not returned", this.testPrefBool.get(this.prefs), is(true));
    }

    @Test
    public void set() {
        SharedPreferences.Editor editor = this.prefs.edit();
        this.testPrefInt.set(editor, 21);
        this.testPrefString.set(editor, "OtherWorld");
        this.testPrefBool.set(editor, false);
        editor.apply();

        assertThat("Integer was not stored", this.prefs.contains(PREFERENCE_KEY_INT), is(true));
        assertThat("Integer is wrong", this.prefs.getInt(PREFERENCE_KEY_INT, -1), is(21));
        assertThat("String was not stored", this.prefs.contains(PREFERENCE_KEY_STRING), is(true));
        assertThat("String is wrong", this.prefs.getString(PREFERENCE_KEY_STRING, "Default"), is("OtherWorld"));
        assertThat("Boolean was not stored", this.prefs.contains(PREFERENCE_KEY_BOOL), is(true));
        assertThat("Boolean is wrong", this.prefs.getBoolean(PREFERENCE_KEY_BOOL, true), is(false));
    }

    @Test
    public void get() {
        SharedPreferences.Editor editor = this.prefs.edit();
        editor.clear();
        editor.putString(PREFERENCE_KEY_STRING, "ThatPlanet");
        editor.putInt(PREFERENCE_KEY_INT, 37);
        editor.putBoolean(PREFERENCE_KEY_BOOL, false);
        editor.apply();

        assertThat("String retrieval was wrong", this.testPrefString.get(this.prefs), is("ThatPlanet"));
        assertThat("Integer retrieval was wrong", this.testPrefInt.get(this.prefs), is(37));
        assertThat("Boolean retrieval was wrong", this.testPrefBool.get(this.prefs), is(false));
    }

    @After
    public void tearDown() {
        this.prefs.edit().clear().apply();
    }
}