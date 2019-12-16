package info.varden.hauk.dialog;

import info.varden.hauk.R;

/**
 * Selection of dialog buttons for custom dialogs.
 *
 * @author Marius Lindvall
 */
public final class Buttons {
    private Buttons() {
    }

    public enum Two {

        OK_CANCEL(R.string.btn_ok, R.string.btn_cancel),
        YES_NO(R.string.btn_yes, R.string.btn_no),
        CREATE_CANCEL(R.string.btn_create, R.string.btn_cancel),
        SETTINGS_DISMISS(R.string.btn_dismiss, R.string.btn_show_settings),
        SETTINGS_OK(R.string.btn_ok, R.string.btn_show_settings),
        OK_SHARE(R.string.btn_ok, R.string.btn_share_short);

        // The dialog has one positive and one negative button.
        private final int positive;
        private final int negative;

        Two(int positive, int negative) {
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

    public enum Three {
        YES_NO_REMEMBER(R.string.btn_yes, R.string.btn_remember, R.string.btn_no);

        // The dialog has one positive, one neutral and one negative button.
        private final int positive;
        private final int neutral;
        private final int negative;

        Three(int positive, int neutral, int negative) {
            this.positive = positive;
            this.neutral = neutral;
            this.negative = negative;
        }

        /**
         * Returns a strings resource ID that corresponds to this button set's positive button.
         */
        public int getPositiveButton() {
            return this.positive;
        }

        /**
         * Returns a strings resource ID that corresponds to this button set's neutral button.
         */
        public int getNeutralButton() {
            return this.neutral;
        }

        /**
         * Returns a strings resource ID that corresponds to this button set's negative button.
         */
        public int getNegativeButton() {
            return this.negative;
        }
    }
}