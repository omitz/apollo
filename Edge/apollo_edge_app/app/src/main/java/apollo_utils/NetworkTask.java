package apollo_utils;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

/**
 * The NetworkTask makes authenticated http requests to the flask-apollo-processor.
 * Note: Networking tasks should not be done on the main thread.
 */
public class NetworkTask extends AsyncTask<URL, Void, Void> {

    private String requestMethod;
    private String filepathOrName;
    private InputStream inputStream;

    /**
     * Constructor
     * @param requestMethod: "PUT" or "POST"
     * @param filepathOrName:
 *                      For an upload, the local file path, eg '/storage/emulated/0/somedir/1.jpg' OR the local file name, eg '1.jpg'.
     *                  For POSTing a message to a rabbitmq route, the S3 path (Re POST functionality/makeHttpRequest, the code currently isn't called,
     *                  but we'll leave the code in place for now in case we want to POST in the future.)
     * @param inputStream:
     *                  In most cases, we're able to perform the http request with only the filepath (in which case inputStream can be null).
     *                  However, not all classes that use this NetworkTask are able to provide a full filepath.
     *                  In those cases, the class using NetworkTask must provide the inputStream.
     */
    public NetworkTask(String requestMethod, String filepathOrName, InputStream inputStream) {
        super();
        this.requestMethod = requestMethod;
        this.filepathOrName = filepathOrName;
        this.inputStream = inputStream;
    }

    @Override
    protected Void doInBackground(URL... urls) {
        // It's always one url. This is simply following the example from the docs.
        for(URL url: urls) {
            try {
                if (requestMethod.equals("PUT")) {
                    makeHttpRequestUpload(url);
                } else if (requestMethod.equals("POST")){
                    makeHttpRequest(url);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response, in which a file is being uploaded
     * https://stackoverflow.com/questions/19026256/how-to-upload-multipart-form-data-and-image-to-server-in-android/26145565#26145565
     */
    private void makeHttpRequestUpload(URL url) throws IOException, JSONException {
        HttpURLConnection connection = getHttpURLConnection(url);
        DataOutputStream outputStream = null;
        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;

        String basename = "";
        FileInputStream fileInputStream = null;
        if (inputStream == null) {
            // Then we should be able to get the stream and name from filepathOrName
            String[] pathEls = filepathOrName.split("/");
            int basenameIndex = pathEls.length - 1;
            basename = pathEls[basenameIndex];
            File file = new File(filepathOrName);
            fileInputStream = new FileInputStream(file);
        } else {
            basename = filepathOrName;
        }

        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        String filefield = "file";
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + basename + "\"" + lineEnd);
        String fileMimeType = URLConnection.guessContentTypeFromName(filepathOrName);
        outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
        outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
        outputStream.writeBytes(lineEnd);

        if (fileInputStream != null) {
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        } else {
            bytesAvailable = inputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            bytesRead = inputStream.read(buffer, 0, bufferSize);
        }
        while (bytesRead > 0) {
            Log.i("NetworkTask", "buffer size: " + bufferSize);
            outputStream.write(buffer, 0, bufferSize);
            if (fileInputStream != null) {
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            } else {
                bytesAvailable = inputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = inputStream.read(buffer, 0, bufferSize);
            }
        }

        outputStream.writeBytes(lineEnd);
        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

        InputStream responseInputStream = null;
        if (200 != connection.getResponseCode()) {
            responseInputStream = connection.getErrorStream();
        } else {
            responseInputStream = connection.getInputStream();
        }
        String jsonResponse = readStream(responseInputStream);
        Log.i("NetworkTask", jsonResponse);

        if (fileInputStream != null) {
            fileInputStream.close();
        } else {
            inputStream.close();
        }
        responseInputStream.close();
        outputStream.flush();
        outputStream.close();
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     * @return jsonResponse
     */
    private String makeHttpRequest(URL url) throws IOException, JSONException {
        HttpURLConnection connection = getHttpURLConnection(url);
        String jsonResponse = "";
        InputStream inputStream = null;
        try {
            // Set up the HTTP request
            connection = (HttpURLConnection) url.openConnection();

            // Set the header
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setConnectTimeout(15000 /* milliseconds */);

            OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
            writeStreamPath(outputStream);
            inputStream = new BufferedInputStream(connection.getInputStream());
            jsonResponse = readStream(inputStream);

            // Actually set up the connection
            connection.connect();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    private HttpURLConnection getHttpURLConnection(URL url) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(requestMethod);
        // Set additional header with authorization
        String authToken = getAuth();
        String bearer = " Bearer " + authToken;
        connection.setRequestProperty("authorization", bearer);
        connection.setDoOutput(true);
        // ChunkedStreamingMode(n) facilitates uploading large files (avoids OOM error)
        connection.setChunkedStreamingMode(1024);
        return connection;
    }

    private void writeStreamPath(OutputStream out) {
        String body = String.format("{\"path\": \"%s\"}", filepathOrName);
        try {
            out.write(body.getBytes(Charset.forName("UTF-8")));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeStreamUsernamePassword(OutputStream out) {
        String body = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", "john user", "johnpassword");
        try {
            out.write(body.getBytes(Charset.forName("UTF-8")));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert the {@link InputStream} into a  String which contains the
     * whole JSON response from the server.
     */
    private String readStream(InputStream inputStream) throws IOException {
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

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     * @return authToken
     */
    private String getAuth() throws IOException, JSONException {
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
            inputStream = new BufferedInputStream(connection.getInputStream());
            jsonResponse = readStream(inputStream);
            // Actually set up the connection
            connection.connect();
        } catch (IOException e) {
            e.printStackTrace();
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
        String authToken = jsonObject.getString("authorization_token");
        Log.i("NetworkTask", authToken);
        return authToken;
    }

}