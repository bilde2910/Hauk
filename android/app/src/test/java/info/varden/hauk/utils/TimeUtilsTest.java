package info.varden.hauk.utils;

import org.junit.Test;

import info.varden.hauk.Constants;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public final class TimeUtilsTest {

    @Test
    public void secondsToTime() {
        assertThat("Negative time improperly rendered", TimeUtils.secondsToTime(-3L), is("-0:03"));
        assertThat("Negative minutes improperly rendered", TimeUtils.secondsToTime(-62L), is("-1:02"));
        assertThat("Single-digit seconds improperly rendered", TimeUtils.secondsToTime(5L), is("0:05"));
        assertThat("Single-digit seconds with minutes improperly rendered", TimeUtils.secondsToTime(62L), is("1:02"));
        assertThat("Two-digit minutes and seconds improperly rendered", TimeUtils.secondsToTime(3227L), is("53:47"));
        assertThat("Hours and single-digit minutes improperly rendered", TimeUtils.secondsToTime(3669L), is("1:01:09"));
    }

    @Test
    public void timeUnitsToSeconds() {
        assertThat("Minutes not properly converted", TimeUtils.timeUnitsToSeconds(2, Constants.DURATION_UNIT_MINUTES), is(120));
        assertThat("Hours not properly converted", TimeUtils.timeUnitsToSeconds(3, Constants.DURATION_UNIT_HOURS), is(10800));
        assertThat("Days not properly converted", TimeUtils.timeUnitsToSeconds(5, Constants.DURATION_UNIT_DAYS), is(432000));
        assertThat("Invalid unit not falling back to seconds", TimeUtils.timeUnitsToSeconds(17, -1), is(17));
    }
}