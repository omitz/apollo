// package apollo_utils;
package com.atakmap.android.apolloedge.apollo_utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.atakmap.android.apolloedge.auth.AuthProvider;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * The NetworkTask makes authenticated http requests to the flask-apollo-processor.
 * Note: Networking tasks should not be done on the main thread.
 */
public class NetworkTask extends AsyncTask<URL, Void, String> {
    protected WeakReference<Context> contextRef;
    protected AuthProvider authProvider;

    // Constructor
    public NetworkTask(Context context) {
        this.contextRef = new WeakReference<>(context);
        this.authProvider = new AuthProvider(this.contextRef.get());
    }

    @Override
    protected String doInBackground(URL... urls) {
        Log.i("NetworkTask", "doInBackground");
        return null;
    }

    protected HttpURLConnection getHttpURLConnection(URL url) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");

        // Set additional header with authorization
        String authToken = authProvider.getToken();
        String bearer = " Bearer " + authToken;
        connection.setRequestProperty("authorization", bearer);
        connection.setDoOutput(true);

        // ChunkedStreamingMode(n) facilitates uploading large files (avoids OOM error)
        connection.setChunkedStreamingMode(1024);
        return connection;
    }

    /**
     * Convert the {@link InputStream} into a  String which contains the
     * whole JSON response from the server.
     */
    protected String readStream(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        if (inputStream != null) {
            // Go from raw data to hr chars. UTF-8 is the unicode character encoding for almost every piece of text you'll find on the web
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            // You wrap an input stream reader in a buffered reader to speed things up
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                line = reader.readLine();
            }
        }
        return stringBuilder.toString();
    }


    // Define interface required to propagate result back to caller
    public interface AsyncResponse {
        void processFinish(String result);
    }

}

