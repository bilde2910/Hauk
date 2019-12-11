package info.varden.hauk.system.preferences.ui.listener;

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;

/**
 * Edit text bind listener that sets the hint of an {@link EditTextPreference}.
 *
 * @author Marius Lindvall
 */
public class HintBindListener implements EditTextPreference.OnBindEditTextListener {
    private final int hintResource;

    public HintBindListener(int hintResource) {
        this.hintResource = hintResource;
    }

    @Override
    public void onBindEditText(@NonNull EditText editText) {
        editText.setHint(this.hintResource);
    }
}
