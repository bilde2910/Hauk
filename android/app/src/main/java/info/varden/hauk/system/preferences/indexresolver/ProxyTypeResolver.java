package info.varden.hauk.system.preferences.indexresolver;

import java.net.Proxy;

import info.varden.hauk.system.preferences.IndexedEnum;

/**
 * An enum representing the various types of proxies available on the system, and their ID when
 * stored in preferences.
 *
 * @author Marius Lindvall
 */
public final class ProxyTypeResolver extends IndexedEnum<ProxyTypeResolver> {
    private static final long serialVersionUID = -2687503543989317320L;

    public static final ProxyTypeResolver SYSTEM_DEFAULT = new ProxyTypeResolver(0, null);
    public static final ProxyTypeResolver DIRECT = new ProxyTypeResolver(1, Proxy.Type.DIRECT);
    public static final ProxyTypeResolver HTTP = new ProxyTypeResolver(2, Proxy.Type.HTTP);
    public static final ProxyTypeResolver SOCKS = new ProxyTypeResolver(3, Proxy.Type.SOCKS);

    /**
     * The type of proxy that this index represents.
     */
    private final Proxy.Type type;

    private ProxyTypeResolver(int index, Proxy.Type type) {
        super(index);
        this.type = type;
    }

    @Override
    public String toString() {
        return "ProxyTypeResolver{" + super.toString() + ",type=" + this.type + "}";
    }

    public Proxy.Type getProxyType() {
        return this.type;
    }
}
