package info.varden.hauk.http.parameter;

/**
 * An enum that identifies the currently active location provider on the device.
 */
public enum LocationProvider {
    FINE(0),
    COARSE(1);

    private final int mode;

    LocationProvider(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return this.mode;
    }

    @Override
    public String toString() {
        return "LocationProvider<mode=" + this.mode + ">";
    }
}
