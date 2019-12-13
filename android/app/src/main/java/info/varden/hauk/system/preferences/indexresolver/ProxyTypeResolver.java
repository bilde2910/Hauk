package info.varden.hauk.system.preferences.indexresolver;

import java.net.Proxy;

/**
 * An enum representing the various types of proxies available on the system, and their ID when
 * stored in preferences.
 *
 * @author Marius Lindvall
 */
@SuppressWarnings("unused")
public final class ProxyTypeResolver extends Resolver<ProxyTypeResolver, Proxy.Type> {
    private static final long serialVersionUID = -2687503543989317320L;

    public static final ProxyTypeResolver SYSTEM_DEFAULT = new ProxyTypeResolver(0, null);
    public static final ProxyTypeResolver DIRECT = new ProxyTypeResolver(1, Proxy.Type.DIRECT);
    public static final ProxyTypeResolver HTTP = new ProxyTypeResolver(2, Proxy.Type.HTTP);
    public static final ProxyTypeResolver SOCKS = new ProxyTypeResolver(3, Proxy.Type.SOCKS);

    private ProxyTypeResolver(int index, Proxy.Type mapping) {
        super(index, mapping);
    }
}
