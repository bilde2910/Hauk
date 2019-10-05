package info.varden.hauk.ui.listener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import info.varden.hauk.R;
import info.varden.hauk.dialog.Buttons;
import info.varden.hauk.dialog.CustomDialogBuilder;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.http.NewLinkPacket;
import info.varden.hauk.struct.Session;
import info.varden.hauk.struct.Share;
import info.varden.hauk.ui.DialogPacketFailureHandler;
import info.varden.hauk.utils.Log;

/**
 * On-click listener for the button that adds a new single-user share link for a running session.
 *
 * @see info.varden.hauk.ui.MainActivity
 * @author Marius Lindvall
 */
public abstract class AddLinkClickListener implements View.OnClickListener {
    /**
     * The activity the button is present on.
     */
    private final Activity act;

    /**
     * A dialog service helper.
     */
    private final DialogService dialogSvc;

    /**
     * The session for which the new share should be created.
     */
    private final Session session;

    /**
     * The default selection on whether or not adoption should be allowed for the newly created
     * link.
     */
    private final boolean defaultAllowAdoption;

    /**
     * Called if the share was successfully created.
     *
     * @param share The share that was created.
     */
    protected abstract void onShareCreated(Share share);

    protected AddLinkClickListener(Activity act, Session session, boolean defaultAllowAdoption) {
        this.act = act;
        this.dialogSvc = new DialogService(act);
        this.session = session;
        this.defaultAllowAdoption = defaultAllowAdoption;
    }

    @Override
    public final void onClick(View view) {
        this.dialogSvc.showDialog(R.string.create_link_title, R.string.create_link_body, Buttons.CREATE_CANCEL, new CustomDialogBuilder() {

            private CheckBox chkAdopt;

            @Override
            public void onPositive() {
                Log.v("User initiated link creation, sending packet..."); //NON-NLS

                // Create a progress dialog while creating the link. This could end up
                // taking a while (e.g. if the host is unreachable, it will eventually time
                // out), and having a progress bar makes for better UX since it visually
                // shows that something is actually happening in the background.
                ProgressDialog progress = new ProgressDialog(AddLinkClickListener.this.act);
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setTitle(R.string.progress_new_link_title);
                progress.setMessage(AddLinkClickListener.this.act.getString(R.string.progress_new_link_body));
                progress.setIndeterminate(true);
                progress.setCancelable(false);
                progress.show();

                new AssociatedPacket(progress, this.chkAdopt.isChecked()).send();
            }

            @Override
            public void onNegative() {
                Log.v("User aborted link creation"); //NON-NLS
            }

            @Override
            public View createView(Context ctx) {
                // Create a dialog that prompts the user for the new share's adoption state.
                Log.v("Inflating view for add-link dialog"); //NON-NLS
                LayoutInflater inflater = AddLinkClickListener.this.act.getLayoutInflater();
                @SuppressWarnings("HardCodedStringLiteral")
                @SuppressLint("InflateParams")
                View dialogView = inflater.inflate(R.layout.dialog_create_link, null);

                // Inherit the adoption state from the main share/saved preference.
                this.chkAdopt = dialogView.findViewById(R.id.dialogNewLinkChkAdopt);
                this.chkAdopt.setChecked(AddLinkClickListener.this.defaultAllowAdoption);

                return dialogView;
            }
        });
    }

    /**
     * The packet sent to the server to request the addition of a new link.
     */
    private final class AssociatedPacket extends NewLinkPacket {
        /**
         * A progress dialog to dismiss when a response has been received for the packet.
         */
        private final ProgressDialog progress;

        /**
         * Creates the packet.
         *
         * @param progress      A progress dialog to dismiss after a response has been received.
         * @param allowAdoption Whether or not this share should be adoptable.
         */
        private AssociatedPacket(ProgressDialog progress, boolean allowAdoption) {
            super(AddLinkClickListener.this.act, AddLinkClickListener.this.session, allowAdoption);
            this.progress = progress;
        }

        @Override
        public void onShareCreated(Share share) {
            Log.i("Share created, showing dialog and sending upstream"); //NON-NLS
            this.progress.dismiss();
            AddLinkClickListener.this.onShareCreated(share);

            // Tell the user that the link was added successfully.
            AddLinkClickListener.this.dialogSvc.showDialog(R.string.link_added_title, R.string.link_added_body);
        }

        @Override
        protected void onFailure(Exception ex) {
            Log.w("Share creation failed", ex); // NON-NLS
            this.progress.dismiss();
            DialogPacketFailureHandler handler = new LinkAdditionFailureHandler(AddLinkClickListener.this.dialogSvc);
            handler.onFailure(ex);
        }
    }

    /**
     * A callback that is called if link creation failed for an additional share. The superclass
     * displays a dialog indicating the failure state to the user.
     */
    private static final class LinkAdditionFailureHandler extends DialogPacketFailureHandler {
        private LinkAdditionFailureHandler(DialogService dialogSvc) {
            super(dialogSvc);
        }

        @Override
        public void onBeforeShowFailureDialog() {
            // TODO: Do something meaningful?
        }
    }
}
