package info.varden.hauk.utils;

import info.varden.hauk.Constants;

/**
 * Time-related utilities.
 *
 * @author Marius Lindvall
 */
public enum TimeUtils {
    ;

    public static final long MILLIS_PER_SECOND = 1000;

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_DAY = 86400;

    /**
     * Converts seconds to an HH:mm:ss string.
     *
     * @param seconds The number of seconds.
     * @return The seconds converted to HH:mm:ss format.
     */
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public static String secondsToTime(long seconds) {
        long hours = seconds / SECONDS_PER_HOUR;
        long min = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        long sec = seconds % SECONDS_PER_MINUTE;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours + ":");
        if (hours > 0 && min < 10) sb.append("0");
        sb.append(min + ":");
        if (sec < 10) sb.append("0");
        sb.append(sec);

        return sb.toString();
    }

    public static int timeUnitsToSeconds(int scalar, int unit) {
        switch (unit) {
            case Constants.DURATION_UNIT_MINUTES:
                return scalar * SECONDS_PER_MINUTE;

            case Constants.DURATION_UNIT_HOURS:
                return scalar * SECONDS_PER_HOUR;

            case Constants.DURATION_UNIT_DAYS:
                return scalar * SECONDS_PER_DAY;

            default:
                return scalar;
        }
    }
}
