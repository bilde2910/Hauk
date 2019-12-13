package info.varden.hauk.http.security;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import info.varden.hauk.utils.Log;

/**
 * Intentionally insecure trust manager that accepts all trust anchors. Should be used with caution.
 */
public final class InsecureTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        Log.v("Client certificate presented for %s", s); //NON-NLS
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        Log.v("Server certificate presented for %s", x509Certificates[0]); //NON-NLS
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        Log.v("Got request for accepted issuers"); //NON-NLS
        return null;
    }

    public static SSLSocketFactory getSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS"); //NON-NLS
        context.init(null, new TrustManager[] {new InsecureTrustManager()}, new SecureRandom());
        return context.getSocketFactory();
    }
}
