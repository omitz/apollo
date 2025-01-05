package com.atakmap.android.apolloedge.apollo_utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class NetworkTaskUpload extends NetworkTask {

    private WeakReference<Context> contextRef;
    private String filepathOrName;
    private InputStream inputStream;
    public AsyncResponse delegate = null;
    private String boundary;
    private final String lineEnd = "\r\n";

    /**
     * Constructor
     * @param filepathOrName:
     *                  The local file path, eg '/storage/emulated/0/somedir/1.jpg' OR the local file name, eg '1.jpg'.
     * @param inputStream:
     *                  In most cases, we're able to perform the http request with only the filepath (in which case inputStream can be null).
     *                  However, not all classes that use this NetworkTask are able to provide a full filepath.
     *                  In those cases, the class using NetworkTask must provide the inputStream.
     */
    public NetworkTaskUpload(Context context, String filepathOrName, InputStream inputStream) {
        super(context);
        this.filepathOrName = filepathOrName;
        this.inputStream = inputStream;
    }

    /**
     * @return result: "success" or "error"
     */
    @Override
    protected String doInBackground(URL... urls) {
        String result = null;
        // It's always one url. This is simply following the example from the docs.
        for (URL url : urls) {
            try {
                result = makeHttpRequestUpload(url);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     *
     * @param requestResult Output of doInBackground
     */
    @Override
    protected void onPostExecute(String requestResult) {
        if (delegate != null) { // The calling activity only has to instantiate a delegate if it needs to receive the requestResult value.
            delegate.processFinish(requestResult);
        }
    }

    /**
     * adds field to form data
     */
    private void addFormField(DataOutputStream outputStream, String key, String value) throws IOException {
        outputStream.writeBytes("--" + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + "ignore_hash" + "\"");
        outputStream.writeBytes(lineEnd);
        outputStream.writeBytes("Content-Type: text/plain; charset=UTF-8");
        outputStream.writeBytes(lineEnd);
        outputStream.writeBytes(lineEnd);
        outputStream.writeBytes("true");
        outputStream.writeBytes(lineEnd);
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response, in which a file is being uploaded
     * https://stackoverflow.com/questions/19026256/how-to-upload-multipart-form-data-and-image-to-server-in-android/26145565#26145565
     */
    private String makeHttpRequestUpload(URL url) throws IOException, JSONException {
        String result = null;
        String error = "error";
        String success = "success";
        HttpURLConnection connection = getHttpURLConnection(url);
        DataOutputStream outputStream = null;
        String twoHyphens = "--";
        boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
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

        try {
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
            this.addFormField(outputStream, "ignore_hash", "true");
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

        } catch (IOException e) {
            // IOException can be thrown when connection is broken (when auth token has expired)
            return error;
        }

        InputStream responseInputStream = null;
        if (200 != connection.getResponseCode()) {
            responseInputStream = connection.getErrorStream();
            result = error;
        } else {
            responseInputStream = connection.getInputStream();
            result = success;
        }
        String jsonResponse = readStream(responseInputStream);

        if (fileInputStream != null) {
            fileInputStream.close();
        } else {
            inputStream.close();
        }
        responseInputStream.close();
        outputStream.flush();
        outputStream.close();
        return result;
    }
}
