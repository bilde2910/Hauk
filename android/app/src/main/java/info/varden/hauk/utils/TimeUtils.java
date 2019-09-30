package info.varden.hauk.utils;

/**
 * Time-related utilities.
 *
 * @author Marius Lindvall
 */
public final class TimeUtils {
    /**
     * Enforce non-instantiable class.
     */
    private TimeUtils() {
        return;
    }

    /**
     * Converts seconds to an HH:mm:ss string.
     *
     * @param seconds The number of seconds.
     * @return The seconds converted to HH:mm:ss format.
     */
    public static String secondsToTime(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h + ":");
        if (h > 0 && m < 10) sb.append("0");
        sb.append(m + ":");
        if (s < 10) sb.append("0");
        sb.append(s);

        return sb.toString();
    }
}
