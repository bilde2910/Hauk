package info.varden.hauk.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.varden.hauk.HaukConst;
import info.varden.hauk.R;
import info.varden.hauk.http.AdoptSharePacket;
import info.varden.hauk.struct.Share;

/**
 * A class that builds a dialog with two input boxes for adopting another share into a group share.
 *
 * @author Marius Lindvall
 */
public abstract class AdoptDialogBuilder extends CustomDialogBuilder {
    private final Context ctx;
    private final Share share;

    public AdoptDialogBuilder(Context ctx, Share share) {
        this.ctx = ctx;
        this.share = share;
    }

    private EditText dialogTxtShare;
    private EditText dialogTxtNick;

    protected abstract void onSuccess(String nick);
    protected abstract void onFailure(Exception ex);

    /**
     * Called when the OK button is clicked in the dialog window.
     */
    @Override
    public final void onPositive() {
        // Get the user data.
        final String nick = this.dialogTxtNick.getText().toString().trim();
        final String adoptID = this.dialogTxtShare.getText().toString().trim();

        // Create a processing dialog, since we are interacting with an external server, which can
        // take some time.
        final ProgressDialog progress = new ProgressDialog(this.ctx);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setTitle(R.string.progress_adopt_title);
        progress.setMessage(String.format(this.ctx.getString(R.string.progress_adopt_body), nick));
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();

        // Send the HTTP request to try and adopt the share.
        new AdoptSharePacket(this.ctx, this.share, adoptID, nick) {
            @Override
            public void onSuccessfulAdoption(String nickname) {
                progress.dismiss();
                AdoptDialogBuilder.this.onSuccess(nickname);
            }

            @Override
            protected void onFailure(Exception ex) {
                progress.dismiss();
                AdoptDialogBuilder.this.onFailure(ex);
            }
        }.send();
    }

    /**
     * Called when the Cancel button is clicked in the dialog window.
     */
    @Override
    public final void onNegative() {
    }

    /**
     * Creates a View that is rendered in the dialog window.
     *
     * @param ctx Android application context.
     * @return A View instance to render on the dialog.
     */
    @Override
    public final View createView(Context ctx) {
        // Ensure input boxes fill the entire width of the dialog.
        TableRow.LayoutParams trParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        trParams.weight = 1F;

        TableLayout layout = new TableLayout(ctx);
        TableRow shareRow = new TableRow(ctx);
        shareRow.setLayoutParams(trParams);
        TableRow nickRow = new TableRow(ctx);
        nickRow.setLayoutParams(trParams);

        TextView textShare = new TextView(ctx);
        textShare.setText(R.string.label_share_url);

        TextView textNick = new TextView(ctx);
        textNick.setText(R.string.label_nickname);

        dialogTxtShare = new EditText(ctx);
        dialogTxtShare.setInputType(InputType.TYPE_CLASS_TEXT);
        dialogTxtShare.setLayoutParams(trParams);
        dialogTxtShare.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // When the share URL text is changed, try to extract the share ID using regex. If
                // a match is found, replace the entire contents with the match.
                String urlMatch = HaukConst.REGEX_ADOPT_ID_FROM_LINK;
                Matcher m = Pattern.compile(urlMatch).matcher(charSequence);
                if (m.find()) {
                    dialogTxtShare.setText(m.group(1));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        dialogTxtNick = new EditText(ctx);
        dialogTxtNick.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        dialogTxtNick.setLayoutParams(trParams);

        shareRow.addView(textShare);
        shareRow.addView(dialogTxtShare);
        nickRow.addView(textNick);
        nickRow.addView(dialogTxtNick);

        layout.addView(shareRow);
        layout.addView(nickRow);

        return layout;
    }
}
