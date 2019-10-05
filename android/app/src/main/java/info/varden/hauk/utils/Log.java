package info.varden.hauk.utils;

/**
 * Log wrapper to simplify logging in Hauk.
 *
 * @author Marius Lindvall
 */
@SuppressWarnings({"unused", "ClassWithTooManyMethods", "OverloadedVarargsMethod"})
public enum Log {
    ;
    private static final int STACK_DEPTH = 4;

    /**
     * Returns the caller of the log function.
     */
    private static String getCaller() {
        return Thread.currentThread().getStackTrace()[STACK_DEPTH].toString();
    }

    /**
     * Converts the given list of objects to a String.format-safe list of strings.
     *
     * @param args The objects to stringify.
     */
    private static Object[] argsToStrings(Object[] args) {
        String[] safeArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            safeArgs[i] = args[i] == null ? "null" : args[i].toString();
        }
        return safeArgs;
    }

    public static void e(String msg) {
        android.util.Log.e(getCaller(), msg);
    }

    public static void e(String msg, Object... args) {
        android.util.Log.e(getCaller(), String.format(msg, argsToStrings(args)));
    }

    public static void e(String msg, Throwable tr) {
        android.util.Log.e(getCaller(), msg, tr);
    }

    public static void e(String msg, Throwable tr, Object... args) {
        android.util.Log.e(getCaller(), String.format(msg, argsToStrings(args)), tr);
    }

    public static void w(String msg) {
        android.util.Log.w(getCaller(), msg);
    }

    public static void w(String msg, Object... args) {
        android.util.Log.w(getCaller(), String.format(msg, argsToStrings(args)));
    }

    public static void w(String msg, Throwable tr) {
        android.util.Log.w(getCaller(), msg, tr);
    }

    public static void w(String msg, Throwable tr, Object... args) {
        android.util.Log.w(getCaller(), String.format(msg, argsToStrings(args)), tr);
    }

    public static void i(String msg) {
        android.util.Log.i(getCaller(), msg);
    }

    public static void i(String msg, Object... args) {
        android.util.Log.i(getCaller(), String.format(msg, argsToStrings(args)));
    }

    public static void i(String msg, Throwable tr) {
        android.util.Log.i(getCaller(), msg, tr);
    }

    public static void i(String msg, Throwable tr, Object... args) {
        android.util.Log.i(getCaller(), String.format(msg, argsToStrings(args)), tr);
    }

    public static void v(String msg) {
        android.util.Log.v(getCaller(), msg);
    }

    public static void v(String msg, Object... args) {
        android.util.Log.v(getCaller(), String.format(msg, argsToStrings(args)));
    }

    public static void v(String msg, Throwable tr) {
        android.util.Log.v(getCaller(), msg, tr);
    }

    public static void v(String msg, Throwable tr, Object... args) {
        android.util.Log.v(getCaller(), String.format(msg, argsToStrings(args)), tr);
    }

    public static void d(String msg) {
        android.util.Log.d(getCaller(), msg);
    }

    public static void d(String msg, Object... args) {
        android.util.Log.d(getCaller(), String.format(msg, argsToStrings(args)));
    }

    public static void d(String msg, Throwable tr) {
        android.util.Log.v(getCaller(), msg, tr);
    }

    public static void d(String msg, Throwable tr, Object... args) {
        android.util.Log.d(getCaller(), String.format(msg, argsToStrings(args)), tr);
    }

    public static void wtf(String msg) {
        android.util.Log.wtf(getCaller(), msg);
    }

    public static void wtf(String msg, Object... args) {
        android.util.Log.wtf(getCaller(), String.format(msg, argsToStrings(args)));
    }

    public static void wtf(String msg, Throwable tr) {
        android.util.Log.wtf(getCaller(), msg, tr);
    }

    public static void wtf(String msg, Throwable tr, Object... args) {
        android.util.Log.wtf(getCaller(), String.format(msg, argsToStrings(args)), tr);
    }
}
