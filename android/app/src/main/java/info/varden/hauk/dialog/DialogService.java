package info.varden.hauk.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.View;

import info.varden.hauk.CustomDialogBuilder;
import info.varden.hauk.R;

/**
 * A helper class for creating dialogs on the main activity.
 *
 * @author Marius Lindvall
 */
public class DialogService {
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
        showDialog(title, message, (Runnable) null);
    }

    /**
     * Shows a dialog box with an OK button.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string resource representing the body of the dialog box.
     * @param onOK    A callback that is run when the user clicks the OK button.
     */
    public void showDialog(int title, int message, final Runnable onOK) {
        showDialog(title, this.ctx.getString(message), onOK);
    }

    /**
     * Shows a dialog box with an OK button.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string representing the body of the dialog box.
     * @param onOK    A callback that is run when the user clicks the OK button.
     */
    public void showDialog(int title, String message, final Runnable onOK) {
        showDialog(this.ctx.getString(title), message, onOK);
    }

    private void showDialog(String title, String message, final Runnable onOK) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this.ctx);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(title);
        dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (onOK != null) onOK.run();
            }
        });
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
    public void showDialog(int title, int message, final Runnable onOK, final Runnable onCancel) {
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
    public void showDialog(int title, String message, final Runnable onOK, final Runnable onCancel) {
        showDialog(this.ctx.getString(title), message, onOK, onCancel);
    }

    private void showDialog(String title, String message, final Runnable onOK, final Runnable onCancel) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this.ctx);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(title);
        dlgAlert.setPositiveButton(this.ctx.getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (onOK != null) onOK.run();
            }
        });
        dlgAlert.setNegativeButton(this.ctx.getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (onCancel != null) onCancel.run();
            }
        });
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    /**
     * Shows a dialog box with OK and Cancel buttons, with a custom rendered View.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string resource representing the body of the dialog box.
     * @param builder A dialog builder that builds a View and handles the dialog buttons.
     */
    public void showDialog(int title, int message, final CustomDialogBuilder builder) {
        showDialog(title, this.ctx.getString(message), builder);
    }

    /**
     * Shows a dialog box with OK and Cancel buttons, with a custom rendered View.
     *
     * @param title   A string resource representing the title of the dialog box.
     * @param message A string representing the body of the dialog box.
     * @param builder A dialog builder that builds a View and handles the dialog buttons.
     */
    public void showDialog(int title, String message, final CustomDialogBuilder builder) {
        showDialog(this.ctx.getString(title), message, builder);
    }

    private void showDialog(String title, String message, final CustomDialogBuilder builder) {
        View view = builder.createView(this.ctx);
        TypedValue tv = new TypedValue();
        int padding = 0;
        if (this.ctx.getTheme().resolveAttribute(R.attr.dialogPreferredPadding, tv, true)) {
            padding = TypedValue.complexToDimensionPixelSize(tv.data, this.ctx.getResources().getDisplayMetrics());
            System.out.println(padding);
        }
        view.setPadding(padding, padding, padding, 0);

        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this.ctx);
        dlgAlert.setMessage(message);
        dlgAlert.setTitle(title);
        dlgAlert.setView(view);

        dlgAlert.setPositiveButton(this.ctx.getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                builder.onOK();
            }
        });
        dlgAlert.setNegativeButton(this.ctx.getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                builder.onCancel();
            }
        });

        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }
}
