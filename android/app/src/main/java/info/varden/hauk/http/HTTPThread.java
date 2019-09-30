package info.varden.hauk.http;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

import info.varden.hauk.BuildConfig;
import info.varden.hauk.struct.Version;

/**
 * An asynchronous task that POSTs data to a given URL with the given POST fields.
 *
 * @author Marius Lindvall
 */
public class HTTPThread extends AsyncTask<HTTPThread.Request, String, HTTPThread.Response> {

    // A callback that is called after the request is completed. Contains received data, or errors,
    // if applicable.
    private final Callback callback;

    // This class is only for use by info.varden.hauk.http.Packet. Other classes should always call
    // the relevant packet to perform a request rather than using HTTPThread directly. This
    // constructor is thus package-level private.
    protected HTTPThread(Callback callback) {
        this.callback = callback;
    }

    @Override
    protected Response doInBackground(Request... data) {
        try {
            // Create a URL-encoded data body for the HTTP request. Only the first request in the
            // array is ever used.
            StringBuilder sb = new StringBuilder();
            boolean first = false;
            for (Map.Entry<String, String> entry : data[0].data.entrySet()) {
                if (first) first = false;
                else sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            // Open a connection to the Hauk server and post the data.
            URL url = new URL(data[0].url);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            client.setConnectTimeout(10000);
            client.setRequestMethod("POST");
            client.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            client.setRequestProperty("User-Agent", "Hauk/" + BuildConfig.VERSION_NAME + " " + System.getProperty("http.agent"));
            client.setDoInput(true);
            client.setDoOutput(true);

            OutputStream os = client.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(sb.toString());
            writer.flush();
            os.close();

            int response = client.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                // The response should be returned as an array of strings where each element of the
                // array is one line of output. Hauk uses this array as an argument array when
                // processing the response.
                String line;
                ArrayList<String> lines = new ArrayList<>();
                BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
                br.close();
                return new Response(null, lines.toArray(new String[lines.size()]), new Version(client.getHeaderField("X-Hauk-Version")));
            } else {
                // Hauk only returns HTTP 200; any other response should be considered an error.
                throw new Exception("Received HTTP " + response + " from server!");
            }
        } catch (Exception ex) {
            // If an exception occurred, return no data.
            return new Response(ex, null, null);
        }
    }

    @Override
    protected void onPostExecute(Response result) {
        // Call the provided callback once a response has been obtained.
        this.callback.run(result);
    }

    /**
     * A structure representing an HTTP POST request. Contains a URL as well as a map of key-value
     * data to be posted to the URL.
     */
    public static class Request {
        private final String url;
        private final Map<String, String> data;

        public Request(String url, Map<String, String> data) {
            this.url = url;
            this.data = data;
        }
    }

    /**
     * A structure representing an HTTP response. Contains either an array of strings representing
     * each line of the response body, or an exception, if one occurred during the request.
     */
    public static class Response {
        private final Exception ex;
        private final String[] data;
        private final Version ver;

        private Response(Exception ex, String[] data, Version ver) {
            this.ex = ex;
            this.data = data;
            this.ver = ver;
        }

        public Exception getException() {
            return this.ex;
        }

        public String[] getData() {
            return this.data;
        }

        public Version getServerVersion() {
            return this.ver;
        }
    }

    /**
     * A callback that is run when the HTTP request is complete. The callback is provided the
     * response.
     */
    public abstract static class Callback {
        public abstract void run(Response resp);
    }
}
