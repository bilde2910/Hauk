package info.varden.hauk.http.security;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Intentionally insecure hostname verifier used to ignore invalid hostnames if hostname validation
 * is disabled for a domain in preferences. Should be used with caution.
 *
 * @author Marius Lindvall
 */
public final class InsecureHostnameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String s, SSLSession sslSession) {
        return true;
    }
}
