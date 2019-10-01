package info.varden.hauk.utils;

/**
 * Time-related utilities.
 *
 * @author Marius Lindvall
 */
public final class TimeUtils {

    public static final int MILLIS_PER_SECOND = 1000;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int SECONDS_PER_HOUR = 3600;
    public static final int SECONDS_PER_DAY = 86400;

    /**
     * Enforce non-instantiable class.
     */
    private TimeUtils() {
    }

    /**
     * Converts seconds to an HH:mm:ss string.
     *
     * @param seconds The number of seconds.
     * @return The seconds converted to HH:mm:ss format.
     */
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public static String secondsToTime(int seconds) {
        int h = seconds / SECONDS_PER_HOUR;
        int m = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        int s = seconds % SECONDS_PER_MINUTE;

        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h + ":");
        if (h > 0 && m < 10) sb.append("0");
        sb.append(m + ":");
        if (s < 10) sb.append("0");
        sb.append(s);

        return sb.toString();
    }
}
