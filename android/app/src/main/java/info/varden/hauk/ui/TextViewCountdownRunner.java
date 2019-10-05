package info.varden.hauk.ui;

import android.widget.TextView;

import androidx.annotation.UiThread;

import info.varden.hauk.utils.TimeUtils;

/**
 * Utility that displays a countdown in a {@link TextView}. Used to display a countdown on the stop
 * button on the UI when a share is running.
 *
 * @author Marius Lindvall
 */
public final class TextViewCountdownRunner extends RepeatingUIThreadTaskExecutor {
    /**
     * The view whose text to update on every tick.
     */
    private final TextView view;

    /**
     * A string format template that represents the text to be put in the text view.
     */
    private final String formatString;

    /**
     * Remaining seconds counter.
     */
    private long counter;

    @UiThread
    public TextViewCountdownRunner(TextView view, String formatString) {
        this.view = view;
        this.formatString = formatString;
    }

    /**
     * Starts the countdown.
     *
     * @param duration The number of seconds to count down for.
     */
    public void start(long duration) {
        this.counter = duration;
        super.start(0L, TimeUtils.MILLIS_PER_SECOND);
    }

    @Override
    public void onTick() {
        if (this.counter >= 0) {
            this.view.setText(String.format(this.formatString, TimeUtils.secondsToTime(this.counter)));
        }
        this.counter--;
    }
}
