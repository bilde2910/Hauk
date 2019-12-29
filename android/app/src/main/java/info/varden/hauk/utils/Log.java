package info.varden.hauk.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import info.varden.hauk.BuildConfig;
import info.varden.hauk.Constants;

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
     * Returns the timestamp and caller of the log function.
     */
    private static String getLogPrefix() {
        String timestamp = new SimpleDateFormat(Constants.DATE_FORMAT_LOG, Locale.US).format(new Date());

        String caller = Thread.currentThread().getStackTrace()[STACK_DEPTH].toString();
        if (caller.startsWith(BuildConfig.APPLICATION_ID)) {
            caller = caller.substring(BuildConfig.APPLICATION_ID.length());
        }
        return timestamp + ": " + caller + ": ";
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
        android.util.Log.e(BuildConfig.APPLICATION_ID, getLogPrefix() + msg);
    }

    public static void e(String msg, Object... args) {
        android.util.Log.e(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)));
    }

    public static void e(String msg, Throwable tr) {
        android.util.Log.e(BuildConfig.APPLICATION_ID, getLogPrefix() + msg, tr);
    }

    public static void e(String msg, Throwable tr, Object... args) {
        android.util.Log.e(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)), tr);
    }

    public static void w(String msg) {
        android.util.Log.w(BuildConfig.APPLICATION_ID, getLogPrefix() + msg);
    }

    public static void w(String msg, Object... args) {
        android.util.Log.w(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)));
    }

    public static void w(String msg, Throwable tr) {
        android.util.Log.w(BuildConfig.APPLICATION_ID, getLogPrefix() + msg, tr);
    }

    public static void w(String msg, Throwable tr, Object... args) {
        android.util.Log.w(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)), tr);
    }

    public static void i(String msg) {
        android.util.Log.i(BuildConfig.APPLICATION_ID, getLogPrefix() + msg);
    }

    public static void i(String msg, Object... args) {
        android.util.Log.i(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)));
    }

    public static void i(String msg, Throwable tr) {
        android.util.Log.i(BuildConfig.APPLICATION_ID, getLogPrefix() + msg, tr);
    }

    public static void i(String msg, Throwable tr, Object... args) {
        android.util.Log.i(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)), tr);
    }

    public static void v(String msg) {
        android.util.Log.v(BuildConfig.APPLICATION_ID, getLogPrefix() + msg);
    }

    public static void v(String msg, Object... args) {
        android.util.Log.v(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)));
    }

    public static void v(String msg, Throwable tr) {
        android.util.Log.v(BuildConfig.APPLICATION_ID, getLogPrefix() + msg, tr);
    }

    public static void v(String msg, Throwable tr, Object... args) {
        android.util.Log.v(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)), tr);
    }

    public static void d(String msg) {
        android.util.Log.d(BuildConfig.APPLICATION_ID, getLogPrefix() + msg);
    }

    public static void d(String msg, Object... args) {
        android.util.Log.d(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)));
    }

    public static void d(String msg, Throwable tr) {
        android.util.Log.v(BuildConfig.APPLICATION_ID, getLogPrefix() + msg, tr);
    }

    public static void d(String msg, Throwable tr, Object... args) {
        android.util.Log.d(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)), tr);
    }

    public static void wtf(String msg) {
        android.util.Log.wtf(BuildConfig.APPLICATION_ID, getLogPrefix() + msg);
    }

    public static void wtf(String msg, Object... args) {
        android.util.Log.wtf(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)));
    }

    public static void wtf(String msg, Throwable tr) {
        android.util.Log.wtf(BuildConfig.APPLICATION_ID, getLogPrefix() + msg, tr);
    }

    public static void wtf(String msg, Throwable tr, Object... args) {
        android.util.Log.wtf(BuildConfig.APPLICATION_ID, getLogPrefix() + String.format(msg, argsToStrings(args)), tr);
    }
}
