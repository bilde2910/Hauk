package info.varden.hauk.service;

/**
 * Proxy class that forwards GNSS activity events to multiple upstream {@link GNSSActiveHandler}s.
 *
 * @author Marius Lindvall
 */
final class MultiTargetGNSSHandlerProxy implements GNSSActiveHandler {
    private final GNSSActiveHandler[] upstream;

    MultiTargetGNSSHandlerProxy(GNSSActiveHandler... upstream) {
        this.upstream = upstream.clone();
    }

    @Override
    public void onCoarseRebound() {
        for (GNSSActiveHandler up : this.upstream) up.onCoarseRebound();
    }

    @Override
    public void onCoarseLocationReceived() {
        for (GNSSActiveHandler up : this.upstream) up.onCoarseLocationReceived();
    }

    @Override
    public void onAccurateLocationReceived() {
        for (GNSSActiveHandler up : this.upstream) up.onAccurateLocationReceived();
    }

    @Override
    public void onServerConnectionLost() {
        for (GNSSActiveHandler up : this.upstream) up.onServerConnectionLost();
    }

    @Override
    public void onServerConnectionRestored() {
        for (GNSSActiveHandler up : this.upstream) up.onServerConnectionRestored();
    }

    @Override
    public void onShareListReceived(String linkFormat, String[] shareIDs) {
        for (GNSSActiveHandler up : this.upstream) up.onShareListReceived(linkFormat, shareIDs);
    }
}
