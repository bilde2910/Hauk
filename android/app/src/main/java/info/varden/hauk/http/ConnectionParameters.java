package info.varden.hauk.http;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.net.Proxy;
import java.net.SocketAddress;

/**
 * Structure used to store connection parameters for backend connections, e.g. proxy details.
 *
 * @author Marius Lindvall
 */
public final class ConnectionParameters implements Serializable {
    private static final long serialVersionUID = -6275381322711990147L;

    /**
     * The type of proxy to use for the connection.
     */
    private final Proxy.Type proxyType;

    /**
     * The proxy endpoint address.
     */
    private final SocketAddress proxyAddress;

    /**
     * The maximum connection timeout, in milliseconds.
     */
    private final int connectTimeout;

    public ConnectionParameters(Proxy.Type proxyType, SocketAddress proxyAddress, int connectTimeout) {
        this.proxyType = proxyType;
        this.proxyAddress = proxyAddress;
        this.connectTimeout = connectTimeout;
    }

    @Nullable
    Proxy getProxy() {
        return this.proxyType == null || this.proxyAddress == null ? null : new Proxy(this.proxyType, this.proxyAddress);
    }

    int getTimeout() {
        return this.connectTimeout;
    }

    @Override
    public String toString() {
        return "ConnectionParameters{"
                + ",proxyType=" + this.proxyType
                + ",proxyAddress=" + this.proxyAddress
                + ",connectTimeout=" + this.connectTimeout
                + "}";
    }
}
