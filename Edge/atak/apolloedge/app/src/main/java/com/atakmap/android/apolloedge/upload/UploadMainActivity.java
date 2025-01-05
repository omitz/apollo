package com.atakmap.android.apolloedge.upload;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.atakmap.android.apolloedge.apollo_utils.ApolloFileUtils;
import com.atakmap.android.apolloedge.apollo_utils.NetworkTask;
import com.atakmap.android.apolloedge.apollo_utils.NetworkTaskUpload;
import com.atakmap.android.apolloedge.apollo_utils.TransferUtils;
import com.atakmap.android.apolloedge.login.LoginActivity;
import com.atakmap.android.apolloedge.plugin.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import static com.atakmap.android.apolloedge.apollo_utils.ApolloConstants.UPLOAD_ENDPOINT_SUFFIX;

public class UploadMainActivity extends AppCompatActivity implements NetworkTask.AsyncResponse {
    private static final int PERMISSIONS_REQUEST = 1;
    private String[] permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    String filepathOrName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_main);
        requestPermissions(permissions, PERMISSIONS_REQUEST);

        Button importBtn = findViewById(R.id.importButton);
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApolloFileUtils.performFileSearch(com.atakmap.android.apolloedge.upload.UploadMainActivity.this, 3);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        InputStream inputStream = null;
        // Check resultCode from ACTION_OPEN_DOCUMENT intent
        if (resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                // Get the file's content URI from the incoming Intent
                Uri uri = data.getData();
                String path = uri.getPath();
                File fileToUpload = new File(Objects.requireNonNull(path));
                final String[] split = fileToUpload.getPath().split(":");
                filepathOrName = Environment.getExternalStorageDirectory() + File.separator + split[1];
                boolean exists = new File(filepathOrName).exists();
                if (!exists) {
                    String msg = String.format("Invalid path from intent: %s. Using stream from uri", path);
                    Log.i("UploadMainActivity", msg);

                    // We can't get the full path from the intent, so we'll use the input stream + the file basename
                    try {
                        inputStream = getApplicationContext().getContentResolver().openInputStream(uri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
                    // Get the column indices of the data in the Cursor, move to the first row in the Cursor and get the data.
                    assert returnCursor != null;
                    int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    returnCursor.moveToFirst();
                    filepathOrName = returnCursor.getString(nameIndex);
                    returnCursor.close();
                }
            }
            URL url = null;
            try {
                String urlStr = TransferUtils.get_api_root_url() + UPLOAD_ENDPOINT_SUFFIX;
                url = new URL(urlStr);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            NetworkTaskUpload uploadTask = new NetworkTaskUpload(this.getApplicationContext(), filepathOrName, inputStream);
            uploadTask.delegate = this;
            uploadTask.execute(url);
        }
    }

    // Override NetworkTaskUpload's AsyncResponse method (NetworkTaskUpload is used in queueUploadViaApi)
    @Override
    public void processFinish(String uploadHttpRequestResult) {
        // Receive the result of onPostExecute
        if (uploadHttpRequestResult != null) {
            if (uploadHttpRequestResult.equals("error")) {
                // Open the login activity
                Intent loginIntent = new Intent(getBaseContext(), LoginActivity.class);
                startActivity(loginIntent);
            } else {
                String msg1 = String.format("Uploaded %s", filepathOrName);
                Toast.makeText(getApplicationContext(), msg1, Toast.LENGTH_LONG).show();
            }
        }
    }
}
