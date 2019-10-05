package info.varden.hauk.ui.listener;

import android.content.Context;
import android.view.View;

import info.varden.hauk.R;
import info.varden.hauk.dialog.AdoptDialogBuilder;
import info.varden.hauk.dialog.Buttons;
import info.varden.hauk.dialog.DialogService;
import info.varden.hauk.struct.Share;
import info.varden.hauk.utils.Log;

/**
 * On-click listener for the button that lets group share hosts adopt existing single-user shares
 * into their group.
 *
 * @see info.varden.hauk.ui.MainActivity
 * @author Marius Lindvall
 */
public final class InitiateAdoptionClickListener implements View.OnClickListener {
    /**
     * Android application context.
     */
    private final Context ctx;

    /**
     * A dialog service helper.
     */
    private final DialogService dialogSvc;

    /**
     * The group share that other shares should be adopted into.
     */
    private final Share share;

    public InitiateAdoptionClickListener(Context ctx, Share share) {
        this.ctx = ctx;
        this.dialogSvc = new DialogService(ctx);
        this.share = share;
    }

    @Override
    public void onClick(View view) {
        Log.v("Creating dialog for adoption"); //NON-NLS
        this.dialogSvc.showDialog(
                R.string.adopt_title,
                String.format(this.ctx.getString(R.string.adopt_body), this.share.getSession().getServerURL()),
                Buttons.OK_CANCEL,
                new AdoptDialogBuilder(this.ctx, this.share) {

            @Override
            public void onSuccess(String nick) {
                InitiateAdoptionClickListener.this.dialogSvc.showDialog(
                        R.string.adopted_title,
                        String.format(InitiateAdoptionClickListener.this.ctx.getString(R.string.adopted_body), nick)
                );
            }

            @Override
            public void onFailure(Exception ex) {
                InitiateAdoptionClickListener.this.dialogSvc.showDialog(
                        R.string.err_server,
                        ex.getMessage()
                );
            }
        });
    }
}
