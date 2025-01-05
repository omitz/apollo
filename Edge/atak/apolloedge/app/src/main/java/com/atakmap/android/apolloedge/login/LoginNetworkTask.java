package com.atakmap.android.apolloedge.login;

import android.content.Context;
import android.util.Log;

import com.atakmap.android.apolloedge.apollo_utils.NetworkTask;
import com.atakmap.android.apolloedge.apollo_utils.TransferUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginNetworkTask extends NetworkTask {

    Login login;
    public AsyncResponse delegate = null;
    String error = "error";

    // Constructor
    public LoginNetworkTask(Context context, Login login) {
        super(context);
        this.login = login;
    }

    /**
     *
     * @param authResult Output of doInBackground
     */
    @Override
    protected void onPostExecute(String authResult) {
        if (delegate != null) { // The calling activity only has to instantiate a delegate if it needs to receive the authResult value.
            delegate.processFinish(authResult);
        }
    }

    @Override
    protected String doInBackground(URL... urls) {
        String authResult;
        try {
            authResult = getAndSaveAuth();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            authResult = error;
        }
        return authResult;
    }

    private void writeStreamUsernamePassword(OutputStream out) {
        String body = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", login.username, login.password);
        try {
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * POST the user-submitted username/password to get an authorization token. Save the authorization token.
     *
     * @return result: Auth token string OR "error"
     */
    private String getAndSaveAuth() throws IOException, JSONException {
        String jsonResponse = "";
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            // Set up the HTTP request
            String urlStr = TransferUtils.get_api_root_url() + "login/";

            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            // Set the header
            connection.setRequestProperty("Content-Type", "application/json");

            // Required for POSTing
            connection.setDoOutput(true);

            connection.setChunkedStreamingMode(0);
            connection.setConnectTimeout(15000 /* milliseconds */);

            OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
            writeStreamUsernamePassword(outputStream);
            if (connection.getResponseCode() < 400) {
                inputStream = new BufferedInputStream(connection.getInputStream());
            } else {
                inputStream = new BufferedInputStream(connection.getErrorStream());
            }
            jsonResponse = readStream(inputStream);
            // Actually set up the connection
            connection.connect();
        } catch (IOException e) {
            e.printStackTrace();
            return error;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        // Extract the auth token from the json
        JSONObject jsonObject = new JSONObject(jsonResponse);
        String authTokenKey = "authorization_token";
        String result;
        if (jsonObject.has(authTokenKey)) {
            String authToken = jsonObject.getString(authTokenKey);
            result = authToken;
            Context context = contextRef.get();
            if (context != null) {
                this.authProvider.saveOrReplaceToken(authToken);
            }
        } else {
            result = error;
        }
        return result;
    }

}
