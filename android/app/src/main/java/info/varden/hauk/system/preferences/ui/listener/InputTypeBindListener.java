package info.varden.hauk.system.preferences.ui.listener;

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;

/**
 * Edit text bind listener that sets the input type of an {@link EditTextPreference}.
 *
 * @author Marius Lindvall
 */
public class InputTypeBindListener implements EditTextPreference.OnBindEditTextListener {
    private final int inputType;

    public InputTypeBindListener(int inputType) {
        this.inputType = inputType;
    }

    @Override
    public void onBindEditText(@NonNull EditText editText) {
        editText.setInputType(this.inputType);
    }
}
