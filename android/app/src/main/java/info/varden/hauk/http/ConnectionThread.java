package info.varden.hauk.http;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import info.varden.hauk.BuildConfig;
import info.varden.hauk.Constants;
import info.varden.hauk.R;
import info.varden.hauk.http.security.CertificateValidationPolicy;
import info.varden.hauk.http.security.InsecureHostnameVerifier;
import info.varden.hauk.http.security.InsecureTrustManager;
import info.varden.hauk.struct.Version;

/**
 * An asynchronous task that HTTP-POSTs data to a given URL with the given POST fields.
 *
 * @author Marius Lindvall
 */
public class ConnectionThread extends AsyncTask<ConnectionThread.Request, String, ConnectionThread.Response> {
    /**
     * The maximum time to wait for the request to complete before the request times out.
     */
    private static final int TIMEOUT = 10000;

    /**
     * A callback that is called after the request is completed. Contains received data, or errors,
     * if applicable.
     */
    private final Callback callback;

    /**
     * This class is only for use by Packet. Other classes should always call the relevant packet to
     * perform a request rather than using ConnectionThread directly. This constructor is thus
     * package-level private.
     *
     * @param callback A callback that is called after the request is completed.
     */
    ConnectionThread(Callback callback) {
        this.callback = callback;
    }

    /**
     * Sends the HTTP request.
     *
     * @param params The request to send. Note: Exactly one request must be provided in this vararg.
     *               All other requests are ignored!
     *
     * @return An HTTP response.
     */
    @Override
    @SuppressWarnings("HardCodedStringLiteral")
    protected final Response doInBackground(Request... params) {
        try {
            Request req = params[0];

            // Configure and open the connection.
            Proxy proxy = req.getParameters().getProxy();
            URL url = new URL(req.getURL());
            HttpURLConnection client = (HttpURLConnection) (proxy == null ? url.openConnection() : url.openConnection(proxy));
            if (url.getHost().endsWith(".onion") && url.getProtocol().equals("https")) {
                // Check if TLS validation should be disabled for .onion addresses over HTTPS.
                if (req.getParameters().getTLSPolicy().equals(CertificateValidationPolicy.DISABLE_TRUST_ANCHOR_ONION)) {
                    ((HttpsURLConnection) client).setSSLSocketFactory(InsecureTrustManager.getSocketFactory());
                } else if (req.getParameters().getTLSPolicy().equals(CertificateValidationPolicy.DISABLE_ALL_ONION)) {
                    ((HttpsURLConnection) client).setSSLSocketFactory(InsecureTrustManager.getSocketFactory());
                    ((HttpsURLConnection) client).setHostnameVerifier(new InsecureHostnameVerifier());
                }
            }

            // Post the data.
            client.setConnectTimeout(req.getParameters().getTimeout());
            client.setRequestMethod("POST");
            client.setRequestProperty("Accept-Language", Locale.getDefault().getLanguage());
            client.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            client.setRequestProperty("User-Agent", "Hauk/" + BuildConfig.VERSION_NAME + " " + System.getProperty("http.agent"));
            client.setDoInput(true);
            client.setDoOutput(true);

            OutputStream os = client.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            writer.write(params[0].getURLEncodedData());
            writer.flush();
            os.close();

            int response = client.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                // The response should be returned as an array of strings where each element of the
                // array is one line of output. Hauk uses this array as an argument array when
                // processing the response.
                String line;
                ArrayList<String> lines = new ArrayList<>();
                BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
                br.close();
                return new Response(null, lines.toArray(new String[0]), new Version(client.getHeaderField(Constants.HTTP_HEADER_HAUK_VERSION)));
            } else {
                // Hauk only returns HTTP 200; any other response should be considered an error.
                return new Response(new ServerException(String.format(params[0].getContext().getString(R.string.err_response_code), String.valueOf(response))), null, null);
            }
        } catch (Exception ex) {
            // If an exception occurred, return no data.
            return new Response(ex, null, null);
        }
    }

    /**
     * Called when the request has been completed.
     *
     * @param result The HTTP response.
     */
    @Override
    protected final void onPostExecute(Response result) {
        // Call the provided callback once a response has been obtained.
        this.callback.run(result);
    }

    /**
     * A structure representing an HTTP POST request. Contains a URL as well as a map of key-value
     * data to be posted to the URL.
     */
    static class Request {
        private final Context ctx;
        private final String url;
        private final Map<String, String> data;
        private final ConnectionParameters params;

        /**
         * Constructs an HTTP request that should be passed through a proxy.
         *
         * @param ctx    Android application context.
         * @param url    The URL to POST data to.
         * @param data   A set of key-value pairs consisting of data to be sent in the POST request.
         * @param params The parameters that should be used when establishing the connection.
         */
        Request(Context ctx, String url, Map<String, String> data, ConnectionParameters params) {
            this.ctx = ctx;
            this.url = url;
            this.data = Collections.unmodifiableMap(data);
            this.params = params;
        }

        private Context getContext() {
            return this.ctx;
        }

        private String getURL() {
            return this.url;
        }

        private ConnectionParameters getParameters() {
            return this.params;
        }

        private String getURLEncodedData() throws UnsupportedEncodingException {
            // Create a URL-encoded data body for the HTTP request.
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : this.data.entrySet()) {
                if (first) first = false;
                else sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            }
            return sb.toString();
        }
    }

    /**
     * A structure representing an HTTP response. Contains either an array of strings representing
     * each line of the response body, or an exception, if one occurred during the request.
     */
    static final class Response {
        private final Exception ex;
        private final String[] data;
        private final Version ver;

        private Response(Exception ex, String[] data, Version ver) {
            this.ex = ex;
            this.data = data;
            this.ver = ver;
        }

        /**
         * Check if an exception was thrown when sending the HTTP request. Returns the exception if
         * one was thrown, null otherwise.
         */
        Exception getException() {
            return this.ex;
        }

        /**
         * Returns an array of data returned in the HTTP response. Each line in this array
         * represents one line of output.
         */
        String[] getData() {
            return this.data.clone();
        }

        /**
         * Returns the Hauk backend version number. Used to check for feature compatibility.
         */
        Version getServerVersion() {
            return this.ver;
        }
    }

    /**
     * A callback that is run when the HTTP request is complete. The callback is provided the
     * response.
     */
    public interface Callback {
        void run(Response resp);
    }
}
