package info.varden.hauk.ui;

import java.io.IOException;
import java.net.MalformedURLException;

import info.varden.hauk.R;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.http.FailureHandler;
import info.varden.hauk.utils.Log;

/**
 * An implementation of {@link FailureHandler} that shows the error in question to the user using a
 * popup dialog.
 *
 * @author Marius Lindvall
 */
public abstract class DialogPacketFailureHandler implements FailureHandler {
    /**
     * Callback that is called before the dialog is showed. Can be used to dismiss other dialogs.
     */
    protected abstract void onBeforeShowFailureDialog();

    private final DialogService dialogSvc;
    private final Runnable onOK;

    /**
     * Constructs the failure handler.
     *
     * @param dialogSvc A dialog helper.
     */
    protected DialogPacketFailureHandler(DialogService dialogSvc) {
        this(dialogSvc, null);
    }

    /**
     * Constructs the failure handler and sets a runnable that will be executed when the dialog is
     * dismissed.
     *
     * @param dialogSvc A dialog helper.
     * @param onOK      A runnable to execute when the error dialog is dismissed.
     */
    DialogPacketFailureHandler(DialogService dialogSvc, Runnable onOK) {
        this.dialogSvc = dialogSvc;
        this.onOK = onOK;
    }

    @Override
    public final void onFailure(Exception ex) {
        onBeforeShowFailureDialog();
        if (ex instanceof MalformedURLException) {
            Log.w("Packet failed to send because of malformed URL", ex); //NON-NLS
            this.dialogSvc.showDialog(R.string.err_client, R.string.err_malformed_url, this.onOK);
        } else if (ex instanceof IOException) {
            Log.e("Packet failed to send due to a connection error", ex); //NON-NLS
            this.dialogSvc.showDialog(R.string.err_connect, ex.getMessage(), this.onOK);
        } else {
            Log.e("Packet was not properly handled by the client or server", ex); //NON-NLS
            this.dialogSvc.showDialog(R.string.err_server, ex.getMessage(), this.onOK);
        }
    }
}
