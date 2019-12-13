package info.varden.hauk.system.preferences.indexresolver;

import java.net.Proxy;

/**
 * An enum representing the various types of proxies available on the system, and their ID when
 * stored in preferences.
 *
 * @author Marius Lindvall
 */
public enum ProxyTypeResolver {
    SYSTEM_DEFAULT(0, null),
    DIRECT(1, Proxy.Type.DIRECT),
    HTTP(2, Proxy.Type.HTTP),
    SOCKS(3, Proxy.Type.SOCKS);

    /**
     * An internal ID for this proxy type that represents the value it is stored as in preferences.
     */
    private final int index;

    /**
     * The type of proxy that this index represents.
     */
    private final Proxy.Type type;

    ProxyTypeResolver(int index, Proxy.Type type) {
        this.index = index;
        this.type = type;
    }

    /**
     * Returns a proxy type by its index.
     *
     * @param index The index of the proxy type.
     * @return A proxy type enum member.
     * @throws EnumConstantNotPresentException if there is no matching type for the given index.
     */
    public static ProxyTypeResolver fromIndex(int index) throws EnumConstantNotPresentException {
        for (ProxyTypeResolver type : ProxyTypeResolver.values()) {
            if (type.getIndex() == index) return type;
        }
        throw new EnumConstantNotPresentException(ProxyTypeResolver.class, "index=" + index);
    }

    @Override
    public String toString() {
        return "ProxyTypeResolver{index=" + this.index + ",type=" + this.type + "}";
    }

    public int getIndex() {
        return this.index;
    }

    public Proxy.Type getProxyType() {
        return this.type;
    }
}
