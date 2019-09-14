package info.varden.hauk.dialog;

import android.content.Context;
import android.view.View;

/**
 * A base class used as a handler to build custom Views and handle events for custom dialogs in the
 * DialogService.
 *
 * @author Marius Lindvall
 */
public abstract class CustomDialogBuilder {
    /**
     * Fires when the positive button is clicked in the dialog.
     */
    public abstract void onPositive();

    /**
     * Fires when the negative button is clicked in the dialog.
     */
    public abstract void onNegative();

    /**
     * A handler to build a View to display in the dialog box.
     *
     * @param ctx Android application context.
     * @return A View to display in the dialog box.
     */
    public abstract View createView(Context ctx);
}
