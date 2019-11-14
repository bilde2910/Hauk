package info.varden.hauk.system.launcher;

import android.content.Context;

/**
 * Base launcher spec used to start an activity.
 *
 * @author Marius Lindvall
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
public interface Launcher {
    /**
     * Starts the activity.
     *
     * @param ctx Android application context.
     */
    void launch(Context ctx);
}
