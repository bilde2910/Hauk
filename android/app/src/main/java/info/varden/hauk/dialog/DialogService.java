package info.varden.hauk.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.View;

import info.varden.hauk.R;
import info.varden.hauk.utils.Log;

/**
 * A helper class for creating dialogs on the main activity.
 *
 * @author Marius Lindvall
 */
@SuppressWarnings({"WeakerAccess", "ClassNamePrefixedWithPackageName"})
public final class DialogService {
    /**
     * Android application context.
     */
    private final Context ctx;

    public DialogService(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Shows a dialog box with an OK button.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string resource representing the body of the dialog box.
     */
    public void showDialog(int title, int message) {
        showDialog(title, this.ctx.getString(message));
    }

    /**
     * Shows a dialog box with an OK button.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string representing the body of the dialog box.
     */
    public void showDialog(int title, String message) {
        showDialog(title, message, null);
    }

    /**
     * Shows a dialog box with an OK button.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string resource representing the body of the dialog box.
     * @param onOK    A callback that is run when the user clicks the OK button.
     */
    public void showDialog(int title, int message, Runnable onOK) {
        showDialog(title, this.ctx.getString(message), onOK);
    }

    /**
     * Shows a dialog box with an OK button.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string representing the body of the dialog box.
     * @param onOK    A callback that is run when the user clicks the OK button.
     */
    public void showDialog(int title, String message, Runnable onOK) {
        showDialog(this.ctx.getString(title), message, onOK);
    }

    /**
     * Shows a dialog box with an OK button.
     *
     * @param title   A string representing the title of the dialog box.
     * @param message A string representing the body of the dialog box.
     * @param onOK    A callback that is run when the user clicks the OK button.
     */
    private void showDialog(String title, String message, Runnable onOK) {
        Log.v("Showing dialog with title=%s, message=%s, run=%s", title, message, onOK); //NON-NLS
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this.ctx);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(title);
        dlgAlert.setPositiveButton(this.ctx.getString(R.string.btn_ok), new RunnableClickListener(onOK));
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    /**
     * Shows a dialog box with OK and Cancel buttons.
     *
     * @param title    A string resource representing the title of the dialog box.
     * @param message  A string resource representing the body of the dialog box.
     * @param onOK     A callback that is run when the user clicks the OK button.
     * @param onCancel A callback that is run when the user clicks the cancel button.
     */
    public void showDialog(int title, int message, Runnable onOK, Runnable onCancel) {
        showDialog(title, this.ctx.getString(message), onOK, onCancel);
    }

    /**
     * Shows a dialog box with OK and Cancel buttons.
     *
     * @param title    A string resource representing the title of the dialog box.
     * @param message  A string representing the body of the dialog box.
     * @param onOK     A callback that is run when the user clicks the OK button.
     * @param onCancel A callback that is run when the user clicks the cancel button.
     */
    public void showDialog(int title, String message, Runnable onOK, Runnable onCancel) {
        showDialog(this.ctx.getString(title), message, onOK, onCancel);
    }

    /**
     * Shows a dialog box with OK and Cancel buttons.
     *
     * @param title    A string representing the title of the dialog box.
     * @param message  A string representing the body of the dialog box.
     * @param onOK     A callback that is run when the user clicks the OK button.
     * @param onCancel A callback that is run when the user clicks the cancel button.
     */
    private void showDialog(String title, String message, Runnable onOK, Runnable onCancel) {
        Log.v("Showing dialog with title=%s, message=%s, run=%s, onCancel=%s", title, message, onOK, onCancel); //NON-NLS
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this.ctx);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(title);
        dlgAlert.setPositiveButton(this.ctx.getString(R.string.btn_ok), new RunnableClickListener(onOK));
        dlgAlert.setNegativeButton(this.ctx.getString(R.string.btn_cancel), new RunnableClickListener(onCancel));
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    /**
     * Shows a dialog box with OK and Cancel buttons, with a custom rendered View.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string resource representing the body of the dialog box.
     * @param buttons The buttons to display on the dialog.
     * @param builder A dialog builder that builds a View and handles the dialog buttons.
     */
    public void showDialog(int title, int message, Buttons buttons, CustomDialogBuilder builder) {
        showDialog(title, this.ctx.getString(message), buttons, builder);
    }

    /**
     * Shows a dialog box with OK and Cancel buttons, with a custom rendered View.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string representing the body of the dialog box.
     * @param buttons The buttons to display on the dialog.
     * @param builder A dialog builder that builds a View and handles the dialog buttons.
     */
    public void showDialog(int title, String message, Buttons buttons, CustomDialogBuilder builder) {
        showDialog(this.ctx.getString(title), message, buttons, builder);
    }

    /**
     * Shows a dialog box with OK and Cancel buttons, with a custom rendered View.
     *
     * @param title   A string representing the title of the dialog box.
     * @param message A string representing the body of the dialog box.
     * @param buttons The buttons to display on the dialog.
     * @param builder A dialog builder that builds a View and handles the dialog buttons.
     */
    private void showDialog(String title, String message, Buttons buttons, CustomDialogBuilder builder) {
        View view = builder.createView(this.ctx);
        if (view != null) {
            TypedValue tv = new TypedValue();
            int padding = 0;
            if (this.ctx.getTheme().resolveAttribute(R.attr.dialogPreferredPadding, tv, true)) {
                padding = TypedValue.complexToDimensionPixelSize(tv.data, this.ctx.getResources().getDisplayMetrics());
            }
            view.setPadding(padding, padding, padding, 0);
        }

        Log.d("Showing dialog with title=%s, message=%s, builder=%s, view=%s", title, message, builder, view); //NON-NLS

        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this.ctx);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(title);
        if (view != null) dlgAlert.setView(view);

        dlgAlert.setPositiveButton(this.ctx.getString(buttons.getPositiveButton()), new PositiveClickListener(builder));
        dlgAlert.setNegativeButton(this.ctx.getString(buttons.getNegativeButton()), new NegativeClickListener(builder));

        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    /**
     * A click listener for a dialog button that calls a given {@link Runnable} when the button is
     * clicked.
     */
    private static final class RunnableClickListener implements DialogInterface.OnClickListener {
        private final Runnable run;

        private RunnableClickListener(Runnable run) {
            this.run = run;
        }

        public void onClick(DialogInterface dialog, int which) {
            Log.v("Closing dialog, which=%s (unknown, run=%s)", which, this.run); //NON-NLS
            if (this.run != null) this.run.run();
        }
    }

    /**
     * A click listener for a dialog button that calls the negative action handler of the given
     * {@link CustomDialogBuilder} when the button is clicked.
     */
    private static final class PositiveClickListener implements DialogInterface.OnClickListener {
        private final CustomDialogBuilder builder;

        private PositiveClickListener(CustomDialogBuilder builder) {
            this.builder = builder;
        }

        public void onClick(DialogInterface dialog, int which) {
            Log.d("Closing dialog, which=%s (positive)", which); //NON-NLS
            this.builder.onPositive();
        }
    }

    /**
     * A click listener for a dialog button that calls the positive action handler of the given
     * {@link CustomDialogBuilder} when the button is clicked.
     */
    private static final class NegativeClickListener implements DialogInterface.OnClickListener {
        private final CustomDialogBuilder builder;

        private NegativeClickListener(CustomDialogBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Log.d("Closing dialog, which=%s (negative)", which); //NON-NLS
            this.builder.onNegative();
        }
    }
}
