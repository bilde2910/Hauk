package info.varden.hauk.ui;

import android.os.Handler;

import androidx.annotation.UiThread;

import java.util.Timer;
import java.util.TimerTask;

/**
 * UI timer class that repeatedly runs a given task on the UI thread.
 *
 * @author Marius Lindvall
 */
abstract class RepeatingUIThreadTaskExecutor {
    /**
     * Android handler for task posting.
     */
    private final Handler handler;

    /**
     * Timer that repeatedly posts to the handler.
     */
    private Timer timer = null;

    /**
     * Called from the UI thread on every tick of the timer.
     */
    protected abstract void onTick();

    @UiThread
    RepeatingUIThreadTaskExecutor() {
        this.handler = new Handler();
    }

    /**
     * Starts the timer with the given delay and interval.
     *
     * @param delay    The delay before first run, in milliseconds.
     * @param interval The interval between each tick, in milliseconds.
     */
    @SuppressWarnings("SameParameterValue") // to ensure future extensibility
    final void start(long delay, long interval) {
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new RepeatingTask(), delay, interval);
    }

    /**
     * Stops the timer and prevents it from ticking further.
     */
    final void stop() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
    }

    /**
     * The timer task that is executed by the timer.
     */
    private final class RepeatingTask extends TimerTask {
        @Override
        public void run() {
            RepeatingUIThreadTaskExecutor.this.handler.post(new Task());
        }
    }

    /**
     * The runnable that is executed by the {@link Handler}. Calls the tick function.
     */
    private final class Task implements Runnable {
        @Override
        public void run() {
            onTick();
        }
    }
}
