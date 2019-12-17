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
        long inputSec = seconds;
        StringBuilder sb = new StringBuilder();
        if (seconds < 0) {
            sb.append("-");
            inputSec *= -1;
        }

        long hours = inputSec / SECONDS_PER_HOUR;
        long min = (inputSec % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        long sec = inputSec % SECONDS_PER_MINUTE;

        if (hours > 0) sb.append(hours + ":");
        if (hours > 0 && min < 10) sb.append("0");
        sb.append(min + ":");
        if (sec < 10) sb.append("0");
        sb.append(sec);

        return sb.toString();
    }

    public static int timeUnitsToSeconds(int scalar, int unit) throws ArithmeticException {
        switch (unit) {
            case Constants.DURATION_UNIT_MINUTES:
                if (Integer.MAX_VALUE / SECONDS_PER_MINUTE < scalar)
                    throw new ArithmeticException(String.format("Integer will overflow when converting %d minutes to seconds", scalar));
                return scalar * SECONDS_PER_MINUTE;

            case Constants.DURATION_UNIT_HOURS:
                if (Integer.MAX_VALUE / SECONDS_PER_HOUR < scalar)
                    throw new ArithmeticException(String.format("Integer will overflow when converting %d hours to seconds", scalar));
                return scalar * SECONDS_PER_HOUR;

            case Constants.DURATION_UNIT_DAYS:
                if (Integer.MAX_VALUE / SECONDS_PER_DAY < scalar)
                    throw new ArithmeticException(String.format("Integer will overflow when converting %d days to seconds", scalar));
                return scalar * SECONDS_PER_DAY;

            default:
                return scalar;
        }
    }
}
