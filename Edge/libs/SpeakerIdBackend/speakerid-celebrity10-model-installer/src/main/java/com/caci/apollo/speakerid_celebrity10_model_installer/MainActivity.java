package com.caci.apollo.speakerid_celebrity10_model_installer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {
    public String[] permissions = new String[] {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    static String TAG = "Tommy Celebrity10";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register GUI callbacks
        Button mButton = findViewById (R.id.install_btn);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Context context = getApplicationContext();
                String outDir = "/sdcard/atak/tools/datapackage/files/ApolloSpeakerID-10VIP/";
                    
                try {
                    ZipInputStream inputStream =
                        new ZipInputStream (context.getAssets().open("ApolloSpeakerID-10VIP.zip"));
                    Log.d (TAG, "calling unzip to " + outDir);
                    unzip (inputStream, new File(outDir));
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d (TAG, Log.getStackTraceString(e));
                    Toast toast = Toast.makeText (context, Log.getStackTraceString(e),
                                                  Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }

                Toast toast = Toast.makeText (context, "SUCCESS: Model installed to " + outDir,
                                              Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }
        });

        // Get storage permission
        if (! hasPermission(permissions)) {
            Log.d (TAG, "requesting storage permission");
            ActivityCompat.requestPermissions (this, permissions, 123);
            return;
        }    
    }

    boolean hasPermission(String[] permissionsList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String permission : permissionsList) {
                boolean granted =
                    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    allGranted = false;
                }
            }
            return allGranted;
        } else {
            return true;
        }
    }

    
    public static void unzip (ZipInputStream zis, File targetDirectory) throws IOException {
        // ref: https://stackoverflow.com/questions/3382996/how-to-unzip-files-programmatically-in-android
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                Log.d (TAG, "unzip  " + ze.getName());
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                                                     dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
                // if time should be restored as well
                long time = ze.getTime();
               if (time > 0)
                   file.setLastModified(time);
            }
        } finally {
            zis.close();
        }
    }
}
