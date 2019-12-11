package info.varden.hauk.system.preferences.ui.listener;

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;

/**
 * Edit text bind listener that cascades the bind event to several
 * {@link androidx.preference.EditTextPreference.OnBindEditTextListener}s.
 *
 * @author Marius Lindvall
 */
public final class CascadeBindListener implements EditTextPreference.OnBindEditTextListener {
    private final EditTextPreference.OnBindEditTextListener[] listeners;

    public CascadeBindListener(EditTextPreference.OnBindEditTextListener[] listeners) {
        this.listeners = listeners.clone();
    }

    @Override
    public void onBindEditText(@NonNull EditText editText) {
        for (EditTextPreference.OnBindEditTextListener listener : this.listeners) {
            listener.onBindEditText(editText);
        }
    }
}
