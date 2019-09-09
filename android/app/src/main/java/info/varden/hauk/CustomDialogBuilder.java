package info.varden.hauk;

import android.content.Context;
import android.view.View;

public abstract class CustomDialogBuilder {
    public abstract void onOK();
    public abstract void onCancel();
    public abstract View createView(Context ctx);
}
