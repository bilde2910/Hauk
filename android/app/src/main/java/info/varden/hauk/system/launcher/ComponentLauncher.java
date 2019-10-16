package info.varden.hauk.system.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Activity starter that launches an intent based on a component name.
 *
 * @author Marius Lindvall
 */
public final class ComponentLauncher implements Launcher {
    /**
     * The package of the activity class to launch.
     */
    private final String packageName;

    /**
     * The activity class to launch.
     */
    private final String className;

    public ComponentLauncher(String packageName, String className) {
        this.packageName = packageName;
        this.className = className.startsWith(".") ? packageName + className : className;
    }

    @Override
    public void launch(Context ctx) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(this.packageName, this.className));
        ctx.startActivity(intent);
    }

    @Override
    public String toString() {
        return "ComponentLauncher{"
                + "packageName=" + this.packageName
                + ",className=" + this.className
                + "}";
    }
}
