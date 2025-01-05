package com.atakmap.android.apolloedge.auth;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class AuthProvider {

    private final Context context;
    //private File tokenFile;

    public AuthProvider(Context context) {
        this.context = context;
        //this.tokenFile = getTokenFile();

    }

    private File getTokenFile() {
        return new File(context.getExternalFilesDir(null), "apollo_token");
    }

    private void writeTokenFile(String authToken) throws IOException {
        FileWriter fw = new FileWriter(getTokenFile().getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(authToken);
        bw.close();
    }

    public void saveOrReplaceToken(String authToken) throws IOException {
        File tokenFile = getTokenFile();
        if (tokenFile.exists()) {
            tokenFile.delete();
        }
        // Create file
        writeTokenFile(authToken);
    }

    public String getToken() {

        FileInputStream inputStream;

        try {
            inputStream = new FileInputStream(getTokenFile());
        } catch (FileNotFoundException e) {
            return null;
        }

        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String receiveStr = "";
        StringBuilder stringBuilder = new StringBuilder();

        try {
            while ((receiveStr = bufferedReader.readLine()) != null) {
                stringBuilder.append(receiveStr);
            }
            inputStream.close();
        } catch (IOException e) {
            Log.e("AuthProvider error", "error reading existing auth file", e);
            return null;
        }

        return stringBuilder.toString();
    }

    public void logout() {
        File tokenFile = getTokenFile();
        if (tokenFile.exists()) {
            tokenFile.delete();
        }
    }
}
