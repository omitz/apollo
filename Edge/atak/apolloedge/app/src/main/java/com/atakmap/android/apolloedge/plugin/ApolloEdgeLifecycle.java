
package com.atakmap.android.apolloedge.plugin;

//import java.io.BufferedReader;
import java.io.File;
//import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
//import java.text.DateFormat;
import java.util.Collection;
//import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
//import java.util.List;

import com.atakmap.android.apolloedge.ApolloEdgeMapComponent;

import com.atakmap.android.importfiles.sort.ImportMissionPackageSort;
// import com.atakmap.android.importfiles.sort.ImportPrefSort;
import com.atakmap.android.apolloedge.auth.AuthProvider;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapView;

import transapps.maps.plugin.lifecycle.Lifecycle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.coremap.log.Log;
import com.caci.apollo.datafeed.DataFeedAPI;



/**
 * In a plugin, the lifecycle and tool components are the only 
 * parts of the NettWarrior plugin architecture that ATAK uses.
 * An ATAK can have zero or more of each of these and they are 
 * defined in the assets folder under the plugins.xml file.
 *
 * A lifecycle roughy maps to the ATAK concept of a MapComponent
 * and is able to add a concrete concept to the ATAK environment.
 * In this case, this lifecycle is responsbile for two 
 * MapComponents.
 */
public class ApolloEdgeLifecycle implements Lifecycle {

    /**************************** Member Variables *****************************/

    private final Context pluginContext;
    private final Collection<MapComponent> overlays;
    private MapView mapView;

    private final static String TAG = "Tommy ApolloEdgeLifecycle";
    private Activity m_activity;
    // private BroadcastReceiver AnalyticStarterReceiver = new AnalyticStarterBroadcastReceiver();
    private String SPEAKER_ID_DATA_PACKAGE = "/sdcard/Android/data/" + // TBF -- use functions
        "com.atakmap.android.apolloedge.plugin/files/ApolloSpeakerID-10VIP.zip";
    private String FACE_ID_DATA_PACKAGE = "/sdcard/Android/data/" + // TBF -- use functions
        "com.atakmap.android.apolloedge.plugin/files/ApolloFaceID-10VIP.zip";
    
    /**************************** CONSTRUCTOR *****************************/

    public ApolloEdgeLifecycle(Context ctx) {
        this.pluginContext = ctx;
        this.overlays = new LinkedList<>();
        this.mapView = null;

    }


    public void setLayoutLoggedIn() {
        Button logout_btn = m_activity.findViewById(R.id.logout_btn);
        Button upload_btn = m_activity.findViewById(R.id.upload_btn);
        TextView login_info_text = m_activity.findViewById(R.id.login_error_text);
        RelativeLayout login_button_wrapper = m_activity.findViewById(R.id.relative_layout_wrapper);

        if (login_info_text != null) login_info_text.setVisibility(View.INVISIBLE);
        if (login_button_wrapper != null) login_button_wrapper.setVisibility(View.GONE);
        if (logout_btn != null) logout_btn.setVisibility(View.VISIBLE);

        if (upload_btn != null) {
            upload_btn.setAlpha(1.0f);

            upload_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ApolloEdgeLifecycle.this.upload();
                }
            });
        }
    }

    public void setLayoutLoggedOut() {
        Button logout_btn = m_activity.findViewById(R.id.logout_btn);
        Button upload_btn = m_activity.findViewById(R.id.upload_btn);
        RelativeLayout login_button_wrapper = m_activity.findViewById(R.id.relative_layout_wrapper);
        TextView login_info_text = m_activity.findViewById(R.id.login_error_text);

        if (login_info_text != null) login_info_text.setVisibility(View.INVISIBLE);
        if (logout_btn != null) logout_btn.setVisibility(View.GONE);
        if (login_button_wrapper != null) login_button_wrapper.setVisibility(View.VISIBLE);

        if (upload_btn != null) {
            upload_btn.setAlpha(0.44f);

            upload_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ApolloEdgeLifecycle.this.loggedout();
                }
            });
        }
    }

    public void upload() {
        Intent intent = new Intent();
        intent.setClassName ("com.atakmap.android.apolloedge.plugin",
                "com.atakmap.android.apolloedge.upload.UploadMainActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this shows a new task
        mapView.getContext().startActivity(intent);
    }

    public void loggedout() {
        TextView login_info_text = m_activity.findViewById(R.id.login_error_text);
        if (login_info_text != null) login_info_text.setVisibility(View.VISIBLE);
    }

    /**************************** INHERITED METHODS *****************************/
    
    @Override
    public void onConfigurationChanged(Configuration arg0) {
        for (MapComponent c : this.overlays)
            c.onConfigurationChanged(arg0);
        Toast.makeText(m_activity.getApplicationContext(), "on configuration changed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate(final Activity arg0,
                         final transapps.mapi.MapView arg1) {
        if (arg1 == null || !(arg1.getView() instanceof MapView)) {
            Log.w(TAG, "This plugin is only compatible with ATAK MapView");
            return;
        }
        this.mapView = (MapView) arg1.getView();
        ApolloEdgeLifecycle.this.overlays.add(new ApolloEdgeMapComponent());
        m_activity = arg0;

        Log.d(TAG, "Calling PluginTemplateLifecycle onCreate");

        // create components
        Iterator<MapComponent> iter = ApolloEdgeLifecycle.this.overlays.iterator();
        MapComponent c;
        while (iter.hasNext()) {
            c = iter.next();
            try {
                c.onCreate(ApolloEdgeLifecycle.this.pluginContext,
                           arg0.getIntent(),
                           ApolloEdgeLifecycle.this.mapView);
            } catch (Exception e) {
                Log.w(TAG,"Unhandled exception trying to create overlays MapComponent", e);
                iter.remove();
            }
        }


        long pluginInstalledTime = 0;
        long pluginLoadedTime = 0;
        try {
            pluginInstalledTime = pluginContext.getPackageManager().
                getPackageInfo("com.atakmap.android.apolloedge.plugin", 0).lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        long currentTime = System.currentTimeMillis();
        String apolloLoadedTsPath = "/sdcard/atak/apolloLoaded.ts";
        File apolloLoadedFile = new File(apolloLoadedTsPath);
        if (!apolloLoadedFile.exists()) {
            Log.e(TAG, "Plugin was never loaded before\n");
            pluginLoadedTime = pluginInstalledTime;
        } else {
            Log.e(TAG, "Plugin loaded before\n");
            String data = null;
            try {
                data = Files.readAllLines(Paths.get(apolloLoadedTsPath)).get(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pluginLoadedTime = Long.parseLong(data);
        }


        /* Update the loaded time */
        String data = Long.toString(currentTime);
        byte[] strToBytes = data.getBytes();
        try {
            Files.write(Paths.get(apolloLoadedTsPath), strToBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
            
        Log.e(TAG, "time now     = " + currentTime);
        Log.e(TAG, "installed = " + pluginInstalledTime);
        Log.e(TAG, "loaded = " + pluginLoadedTime);

        if (pluginInstalledTime >= pluginLoadedTime) {
            // Never loaded before. 
            Log.e(TAG, "calling FIRSTRUN");

            // Copy model config file to storage:
            try {
                // writeAssetFileToStorage (APOLLO_MODEL_CONFIG_FILE); // for data feed
                writeAssetFileToStorage (SPEAKER_ID_DATA_PACKAGE); //
                writeAssetFileToStorage (FACE_ID_DATA_PACKAGE); //
            } catch (IOException e) {
                Toast.makeText(mapView.getContext(),
                               "ERROR: Some files are missing from the Assets folder.",
                               Toast.LENGTH_LONG).show();
                e.printStackTrace();
                Log.e(TAG, android.util.Log.getStackTraceString(e));
                Log.w(TAG, "Some files are missing from the Assets folder");
                throw new RuntimeException("This is a crash");
            }

            // Install Data Packages
            String[] filePaths = {SPEAKER_ID_DATA_PACKAGE, FACE_ID_DATA_PACKAGE};
            for (String filePath : filePaths) {
                File f = new File (filePath);

                ImportMissionPackageSort importer = new
                    ImportMissionPackageSort.ImportMissionV1PackageSort
                    (mapView.getContext(), true, true, false);
                if (!importer.match (f)) {
                    Toast.makeText (mapView.getContext(),
                                    "failure [1]: " + SPEAKER_ID_DATA_PACKAGE,
                                    Toast.LENGTH_SHORT).show();
                } else {
                    boolean success = importer.beginImport(f);
                    if (! success) {
                        Toast.makeText (mapView.getContext(),
                                        "failure [2]: " + f,
                                        Toast.LENGTH_LONG).show();
                    }
                }
            }
            
        } else {
            // Here the user had loaded the plugin after the install.
            
            Log.e(TAG, "NOT FIRSTRUN..");
        }

        // Finally initialize DataFeedAPI
        Log.d(TAG, "Calling DataFeedAPI init");
        DataFeedAPI.init (mapView, pluginContext);
        Log.d(TAG, "Done Calling DataFeedAPI");
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "Calling ApolloEdgeLifecycle onDestroy");

        Log.d(TAG, "Destroying DataFeed");
        DataFeedAPI.dispose();
        Log.d(TAG, "Done destroying DataFeed");

        for (MapComponent c : this.overlays)
            {
                Log.d(TAG, "destroying olverlay mapcomponenet " + c);
                c.onDestroy (this.pluginContext, this.mapView);
                Log.d(TAG, "done destroying olverlay mapcomponenet " + c);
            }

        Log.d(TAG, "Done Calling ApolloEdgeLifecycle onDestroy");
    }

    @Override
    public void onFinish() {
        // XXX - no corresponding MapComponent method
        Log.d(TAG, "Calling ApolloEdgeLifecycle onFinish");
    }

    @Override
    public void onPause() {
        for (MapComponent c : this.overlays)
            c.onPause(this.pluginContext, this.mapView);
    }

    @Override
    public void onResume() {
        for (MapComponent c : this.overlays)
            c.onResume(this.pluginContext, this.mapView);

        AuthProvider authProvider = new AuthProvider(this.pluginContext);
        String token = authProvider.getToken();
        if (token != null) {
            setLayoutLoggedIn();
        }
        else {
            setLayoutLoggedOut();
        }

    }

    @Override
    public void onStart() {
        for (MapComponent c : this.overlays)
            c.onStart(this.pluginContext, this.mapView);
    }

    @Override
    public void onStop() {
        for (MapComponent c : this.overlays)
            c.onStop(this.pluginContext, this.mapView);
    }

    
    /************************* Helper Methods *************************/


    private void writeAssetFileToStorage(String FILE_PATH) throws IOException {
        Log.d(TAG, ">> writeFileToStorage");
        Context mContext = this.pluginContext;
        
        AssetManager assetManager = mContext.getAssets();
        File fileFILE = new File (FILE_PATH);
//        try {
            Files.createDirectories(Paths.get(fileFILE.getParent()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        String fileName = fileFILE.getName();
        Log.d(TAG, ">> writeFileToStorage assset " + fileName + " " + "to " + FILE_PATH);
//        try (InputStream input = assetManager.open(fileName);
//             OutputStream output = new FileOutputStream(FILE_PATH)) {
            InputStream input = assetManager.open(fileName);
            OutputStream output = new FileOutputStream(FILE_PATH);
            byte[] buffer = new byte[input.available()];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            Log.e(TAG, "File is not found");
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.d(TAG, "Error while writing the file");
//        }
        
        Log.d(TAG, "<< writeFileToStorage");
    }
}
