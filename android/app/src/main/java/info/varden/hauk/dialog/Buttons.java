package info.varden.hauk.dialog;

import info.varden.hauk.R;

/**
 * Selection of dialog buttons for custom dialogs.
 *
 * @author Marius Lindvall
 */
public enum Buttons {

    OK_CANCEL       (R.string.btn_ok, R.string.btn_cancel),
    YES_NO          (R.string.btn_yes, R.string.btn_no),
    CREATE_CANCEL   (R.string.btn_create, R.string.btn_cancel);

    // The dialog has one positive and one negative button.
    private final int positive;
    private final int negative;

    Buttons(int positive, int negative) {
        this.positive = positive;
        this.negative = negative;
    }

    /**
     * Returns a strings resource ID that corresponds to this button set's positive button.
     */
    public int getPositiveButton() {
        return this.positive;
    }

    /**
     * Returns a strings resource ID that corresponds to this button set's negative button.
     */
    public int getNegativeButton() {
        return this.negative;
    }
}
