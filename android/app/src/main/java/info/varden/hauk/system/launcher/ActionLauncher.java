package info.varden.hauk.system.launcher;

import android.content.Context;
import android.content.Intent;

/**
 * Activity starter that launches an intent by an action name.
 *
 * @author Marius Lindvall
 */
public final class ActionLauncher implements Launcher {
    /**
     * The name of the action to launch.
     */
    private final String action;

    public ActionLauncher(String action) {
        this.action = action;
    }

    @Override
    public void launch(Context ctx) {
        Intent intent = new Intent(this.action);
        ctx.startActivity(intent);
    }

    @Override
    public String toString() {
        return "ActionLauncher{"
                + "action=" + this.action
                + "}";
    }
}
