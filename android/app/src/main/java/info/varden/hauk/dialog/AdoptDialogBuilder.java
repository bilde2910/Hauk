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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.varden.hauk.CustomDialogBuilder;
import info.varden.hauk.HTTPThread;
import info.varden.hauk.R;

public abstract class AdoptDialogBuilder extends CustomDialogBuilder {
    private final Context ctx;
    private final String serverURL;
    private final String sessionID;
    private final String groupPIN;

    public AdoptDialogBuilder(Context ctx, String serverURL, String sessionID, String groupPIN) {
        this.ctx = ctx;
        this.serverURL = serverURL;
        this.sessionID = sessionID;
        this.groupPIN = groupPIN;
    }

    private EditText diagTxtShare;
    private EditText diagTxtNick;

    public abstract void onSuccess(String nick);
    public abstract void onFailure(Exception ex);

    @Override
    public final void onOK() {
        final String nick = this.diagTxtNick.getText().toString().trim();
        final String adoptID = this.diagTxtShare.getText().toString().trim();

        final ProgressDialog prog = new ProgressDialog(this.ctx);
        prog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        prog.setTitle(R.string.prog_adopt_title);
        prog.setMessage(String.format(this.ctx.getString(R.string.prog_adopt_body), nick));
        prog.setIndeterminate(true);
        prog.setCancelable(false);
        prog.show();

        HashMap<String, String> data = new HashMap<>();
        data.put("sid", this.sessionID);
        data.put("nic", nick);
        data.put("aid", adoptID);
        data.put("pin", this.groupPIN);

        HTTPThread req = new HTTPThread(new HTTPThread.Callback() {
            @Override
            public void run(HTTPThread.Response resp) {
                prog.dismiss();
                Exception e = resp.getException();
                if (e == null) {
                    String[] data = resp.getData();
                    if (data.length < 1) {
                        onFailure(new Exception(AdoptDialogBuilder.this.ctx.getString(R.string.err_empty)));
                    } else {
                        if (data[0].equals("OK")) {
                            onSuccess(nick);
                        } else {
                            StringBuilder err = new StringBuilder();
                            for (String line : data) {
                                err.append(line);
                                err.append("\n");
                            }
                            onFailure(new Exception(err.toString()));
                        }
                    }
                } else {
                    onFailure(e);
                }
            }
        });
        req.execute(new HTTPThread.Request(this.serverURL + "api/adopt.php", data));
    }

    @Override
    public final void onCancel() {
        return;
    }

    @Override
    public final View createView(Context ctx) {
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

        diagTxtShare = new EditText(ctx);
        diagTxtShare.setInputType(InputType.TYPE_CLASS_TEXT);
        diagTxtShare.setLayoutParams(trParams);
        diagTxtShare.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                return;
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String urlMatch = "\\?([A-Za-z0-9-]+)";
                Matcher m = Pattern.compile(urlMatch).matcher(charSequence);
                if (m.find()) {
                    diagTxtShare.setText(m.group(1));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                return;
            }
        });

        diagTxtNick = new EditText(ctx);
        diagTxtNick.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        diagTxtNick.setLayoutParams(trParams);

        shareRow.addView(textShare);
        shareRow.addView(diagTxtShare);
        nickRow.addView(textNick);
        nickRow.addView(diagTxtNick);

        layout.addView(shareRow);
        layout.addView(nickRow);

        return layout;
    }
}
