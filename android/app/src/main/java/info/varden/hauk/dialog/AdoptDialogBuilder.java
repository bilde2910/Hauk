package info.varden.hauk.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.http.AdoptSharePacket;
import info.varden.hauk.struct.Share;
import info.varden.hauk.utils.Log;

/**
 * A class that builds a dialog with two input boxes for adopting another share into a group share.
 *
 * @author Marius Lindvall
 */
public abstract class AdoptDialogBuilder implements CustomDialogBuilder {
    private final Context ctx;
    private final Share share;

    /**
     * Constructs the builder.
     *
     * @param ctx   Android application context.
     * @param share The share that should be adopted into.
     */
    protected AdoptDialogBuilder(Context ctx, Share share) {
        this.ctx = ctx;
        this.share = share;
    }

    /**
     * The text input box that will contain the sharing link entered or pasted by the user.
     */
    private EditText dialogTxtShare;

    /**
     * The text input box that will contain the adopted user's nickname.
     */
    private EditText dialogTxtNick;

    /**
     * Called if the share was successfully adopted.
     *
     * @param nick The nickname assigned to the share by the user.
     */
    protected abstract void onSuccess(String nick);

    /**
     * Called if the share could not be adopted.
     *
     * @param ex An exception indicating the reason why the share could not be adopted.
     */
    protected abstract void onFailure(Exception ex);

    /**
     * Called when the OK button is clicked in the dialog window.
     */
    @Override
    public final void onPositive() {
        // Get the user data.
        String nick = this.dialogTxtNick.getText().toString().trim();
        String adoptID = this.dialogTxtShare.getText().toString().trim();

        Log.v("User initiated adoption with nick=%s, id=%s", nick, adoptID); //NON-NLS

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
                Log.i("Adoption was successful for nick=%s", nickname); //NON-NLS
                progress.dismiss();
                AdoptDialogBuilder.this.onSuccess(nickname);
            }

            @Override
            protected void onFailure(Exception ex) {
                Log.w("Adoption failed", ex); //NON-NLS
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
        Log.v("User aborted adoption"); //NON-NLS
    }

    /**
     * Creates a View that is rendered in the dialog window.
     *
     * @param ctx Android application context.
     * @return A View instance to render on the dialog.
     */
    @Override
    public final View createView(Context ctx) {
        // TODO: Inflate this instead
        // Ensure input boxes fill the entire width of the dialog.
        TableRow.LayoutParams trParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT);
        trParams.weight = 1.0F;

        TableLayout layout = new TableLayout(ctx);
        TableRow shareRow = new TableRow(ctx);
        shareRow.setLayoutParams(trParams);
        TableRow nickRow = new TableRow(ctx);
        nickRow.setLayoutParams(trParams);

        TextView textShare = new TextView(ctx);
        textShare.setText(R.string.label_share_url);

        TextView textNick = new TextView(ctx);
        textNick.setText(R.string.label_nickname);

        this.dialogTxtShare = new EditText(ctx);
        this.dialogTxtShare.setInputType(InputType.TYPE_CLASS_TEXT);
        this.dialogTxtShare.setLayoutParams(trParams);
        this.dialogTxtShare.addTextChangedListener(new LinkIDMatchReplacementListener(this.dialogTxtShare));

        this.dialogTxtNick = new EditText(ctx);
        this.dialogTxtNick.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        this.dialogTxtNick.setLayoutParams(trParams);

        shareRow.addView(textShare);
        shareRow.addView(this.dialogTxtShare);
        nickRow.addView(textNick);
        nickRow.addView(this.dialogTxtNick);

        layout.addView(shareRow);
        layout.addView(nickRow);

        return layout;
    }

    /**
     * A watcher class that watches a text input box and replaces its contents with the
     * corresponding link ID if a view link is entered or pasted into the box by the user.
     */
    private static final class LinkIDMatchReplacementListener implements TextWatcher {
        /**
         * The text input box that should be watched and updated.
         */
        private final EditText inputBox;

        private LinkIDMatchReplacementListener(EditText inputBox) {
            this.inputBox = inputBox;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            // When the share URL text is changed, try to extract the share ID using regex. If
            // a match is found, replace the entire contents with the match.
            Matcher matcher = Pattern.compile(Constants.REGEX_ADOPT_ID_FROM_LINK).matcher(charSequence);
            if (matcher.find()) {
                Log.i("Found possible link ID %s in URL %s; replacing", matcher.group(1), charSequence); //NON-NLS
                this.inputBox.setText(matcher.group(1));
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }
}
