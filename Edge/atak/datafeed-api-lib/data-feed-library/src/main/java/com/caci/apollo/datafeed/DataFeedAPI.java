package com.caci.apollo.datafeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.importfiles.sort.ImportPrefSort;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.MissionAPI;
import com.atakmap.android.missionapi.data.FeedManager;
import com.atakmap.android.missionapi.data.LocalFeedProvider;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.get.GetFeedFileRequest;
import com.atakmap.android.missionapi.net.http.get.GetFeedRequest;
import com.atakmap.android.missionapi.net.http.get.GetFeedsListRequest;
import com.atakmap.android.missionapi.net.http.listener.SimpleRequestListener;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.TAKServerListener;
import com.atakmap.comms.app.CotPortListActivity.CotPort;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.foxykeep.datadroid.requestmanager.Request;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.atakmap.android.maps.MapView.getMapView;
import static com.atakmap.android.missionapi.net.http.RestManager.RESPONSE_JSON;
import static com.atakmap.android.missionapi.util.FeedUtils.toast;
import static com.caci.apollo.datafeed.AnalyticStarterBroadcastReceiver.ANALYTIC_STARTER_BROADCAST;



/**
 * The main class used to initialize Data Feed library
 */
public class DataFeedAPI {

    /*******************************
     *          Constants          *
     *******************************/
    private static final String TAG = "Tommy DataFeedAPI";

    /*******************************
     *      Member Variables       *
     *******************************/
    private static String APOLLO_MODEL_CONFIG_FILE = "analytic_model_info.json";
    private static String SERVER_NCS = null;  // See APOLLO_MODEL_CONFIG_FILE
    private static String FEED_NAME = null;   //  ..
    private static TAKServerListener m_takServerListener = TAKServerListener.getInstance();
    private static GetFeedRequest m_getFeedReq = null;
    private static RestManager m_restManager = RestManager.getInstance();
    private static Feed m_missionFeed = null;
    private static FeedManager m_feedManager = FeedManager.getInstance();
    private static AnalyticModelData m_analyticModel = new AnalyticModelData();
    private static Context m_pluginContext = null;

    private static feedRequestListenerClass m_feedReqListener = new feedRequestListenerClass();
    private static feedFileRequestListenerClass m_feedFileReqListener =
        new feedFileRequestListenerClass();

    private static BroadcastReceiver m_analyticStarterReceiver =
        new AnalyticStarterBroadcastReceiver();

    private static DataFeedAPI m_instance = null;


    /*
     * This listener is used to detect server connectivity.  It gets
     * called whenever TAK server is connected or disconnected.  Once
     * the server is connected, we immediately subscribe to the
     * mission feed
     */
    private static CotServiceRemote.OutputsChangedListener m_outputsChangedListener = null;
    
    /*******************************
     *      Data Structure        *
     *******************************/
    private static class feedListRequestListenerClass extends SimpleRequestListener {
        @Override
        public void onRequestResult(Request request, Bundle resultData, int responseCode) {
            Log.d(TAG, "got FeedList Request Result");
            Log.d (TAG, "feedlistRequest (responseCode) " + responseCode);
            if ((responseCode == -1) || (resultData == null)) {
                Log.d (TAG, "Wifi is not on?");
                return;
            }
            Log.d(TAG, "result = " + resultData.toString()); // could be null
            
            List<Feed> feedList = m_restManager.getFeeds
                (AbstractRequest.fromRequest(request), resultData);
            
            for (Feed feedElm : feedList) {
                if (feedElm.getName().equals(FEED_NAME)) {
                    Log.d (TAG, "Can subscribed to feed " + FEED_NAME);

                    // Now we verified feed is available, we subscribe to it.
                    Log.d (TAG, "Found feed " + FEED_NAME);
                    m_getFeedReq = new GetFeedRequest(feedElm);
                    Log.d(TAG, "getFeedReq.isValid() " + m_getFeedReq.isValid());
                    Log.d(TAG, "getFeedReq.endPoint = " + m_getFeedReq.getRequestEndpoint(""));
                    m_restManager.executeRequest (m_getFeedReq, m_feedReqListener);
                    return;
                }
            }

            Log.d (TAG, "Did not find feed " + FEED_NAME);
        }
    }


    /*
     * Gets called when the TAK server returns a list of available
     * feeds
     */
    private static class feedRequestListenerClass extends SimpleRequestListener {
        @Override
        public void onRequestResult(Request request, Bundle resultData, int responseCode) {
            Log.d (TAG, "Trying to get feed");
            if (m_getFeedReq == null || resultData == null) {
                Log.d (TAG, "feed is not available (resultData) " + resultData);
                Log.d (TAG, "feed is not available (responseCode) " + responseCode);
                return;
            }
            m_missionFeed = m_restManager.getFeed (m_getFeedReq, resultData);
            Log.d (TAG, "missionFeed is " + m_missionFeed);
            if (m_missionFeed == null) {
                Log.d (TAG, "missionFeed not found " + request);
                return;
            }

            // join and subscribe to a mission
            m_missionFeed.setSubscribed (true);
            m_missionFeed.syncMission();
            
            myLocalFeedProviderClass myLocalFeedProvider = new myLocalFeedProviderClass();
            myLocalFeedProvider.init (m_missionFeed);
            m_feedManager.init ((LocalFeedProvider) myLocalFeedProvider);

            // Set local path for feed
            List<FeedFile> fileLst = m_missionFeed.getFiles();
            for (int idx = 0; idx < m_missionFeed.getContentCount(); idx++) {
                FeedFile feedFile = fileLst.get(idx);
                
                File feedFILE = m_missionFeed.getDefaultFilePath(feedFile);
                feedFile.setLocalPath (feedFILE.getAbsolutePath());
                Log.d (TAG, "feedFILE abs path = " + feedFILE.getAbsolutePath());
                Log.d (TAG, "feedFile local path = " + feedFile.getLocalPath());
                
                if (! feedFile.isStoredLocally()) {
                    Log.d(TAG, "Downloading = " + feedFile.getName());
                    GetFeedFileRequest getFeedFileReq;
                    getFeedFileReq = new GetFeedFileRequest (m_missionFeed, feedFile, true);
                    m_restManager.executeRequest(getFeedFileReq, m_feedFileReqListener);
                }
                else {
                    Log.d(TAG, "Skip downloading (alrady download previously) = " +
                          feedFile.getName());
                    CheckDownloadFile (feedFile.getLocalFile());
                }
            }
        }
    }


    private static class myLocalFeedProviderClass implements LocalFeedProvider {
        protected List<Feed> feedList = new ArrayList<>();
        public void init(Feed feed) {
            this.feedList.add(feed);
        }
        
        @Override
        public List<Feed> getFeeds(){
            return this.feedList;
        }
    }


    /*
     * This listener gets called when the sever returns a list of
     * files inside the mission feed.
     */
    private static class feedFileRequestListenerClass extends SimpleRequestListener {
        @Override
        public void onRequestResult(Request request, Bundle resultData, int responseCode) {
            if (resultData == null) {
                Log.d (TAG, "file download failed? " + request + " code = " + responseCode);
                return;
            }

            File fileFILE = new File (resultData.getString(RESPONSE_JSON));

            // toast("Downloaded file = " + fileFILE.getName());
            Log.d (TAG, "Downloaded file = " + fileFILE.getAbsolutePath());

            CheckDownloadFile (fileFILE);
        }
    }
    

    /*
     * Handle CoT Event for Mission feed from TAK Server.  A CoT
     * event gets sent when the TAKserver wants to notify the client
     * any change in mission feed.  The change could be "CREATE",
     * "DELECTE", "MissionChanges", etc.
     */
    private static CotServiceRemote.CotEventListener m_cotEventListner =
        new CotServiceRemote.CotEventListener() {
            @Override
            public void onCotEvent(CotEvent event, Bundle extra) {

                Log.d(TAG, "got CotEvent sent to me " + event);
                CotDetail cotDetail = event.getDetail();

                /* look for any apollo feed event */
                if (! cotDetail.toString().contains(String.format("name='%s'", FEED_NAME))) {
                    Log.d(TAG, "can't find " + FEED_NAME + " related Feed event");
                    return;
                }

                /* just sync everything */
                Log.d(TAG, "Ignore actual event type (MissionChanges, CREATE, DELETE) " +
                      "  just blindly sync");
                SubscribeAndDownload (SERVER_NCS, FEED_NAME);
            }
        };


    
    /*******************************
     *      Inherited Functions    *
     *******************************/

    
    /*******************************
     *      Member Functions       *
     *******************************/

    /**
     * @hidden
     */
    public DataFeedAPI() {
        // Data Feed is meant to be a static classs, don't show it in javadoc
    }
    
    /**
     * Initialize the API.  
     * Load the default config file 'analytic_model_info.json' from the assets folder.
     * @param mapView the ATAK mapview
     * @param pluginCtx the ATAK Plugin context
     *
     * Assume
     */
    public static void init(MapView mapView, Context pluginCtx) {
        init(mapView, pluginCtx, null);
    }

    /**
     * Initialize the API.  
     * This version allows loading config file from different location.
     * @param mapView the ATAK mapview
     * @param pluginCtx the ATAK Plugin context
     * @param installConfigFilePath the absolute path of the config file to install.
     *                              Set it to null will load 'analytic_model_info.json'
     *                              from the assets folder.
     */
    public static void init (MapView mapView, Context pluginCtx, String installConfigFilePath) {
        Log.d (TAG, "calling init");
        MissionAPI.init (mapView, pluginCtx);
        m_pluginContext = pluginCtx;

        // Register the broadcast receiver(s)
        IntentFilter filter = new IntentFilter (ANALYTIC_STARTER_BROADCAST); // single action
        m_pluginContext.registerReceiver (m_analyticStarterReceiver, filter);

        // Copy off the config file from Assets only when first installed
        String configFilePath = getTmpFile ("", APOLLO_MODEL_CONFIG_FILE);
        File configFILE = new File(configFilePath);
        Boolean pluginAlreadyInstalled = configFILE.exists();
        Log.d (TAG, "pluginAlreadyInstalled = " + pluginAlreadyInstalled);
        if (! pluginAlreadyInstalled) {

            if (installConfigFilePath == null) {
                AssetManager mgr = pluginCtx.getAssets();
                configFilePath = copyAsset (mgr, "", APOLLO_MODEL_CONFIG_FILE);
                if (null == configFilePath) {
                    Log.d (TAG, "Could not copy config file!");
                    throw new RuntimeException("This is a crash");
                }
            } else {
                // use custom provided config file
                File srcFile = new File(installConfigFilePath);
                File destFile = new File (configFilePath);
                try {
                    copyFileUsingStream (srcFile, destFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("This is a crash");
                }
            }
            Log.d (TAG, "configFilePath = " + configFilePath);
        }

        // Load analytics model info first:
        m_analyticModel.Read (configFilePath); // TBF -- need to check existence
        SERVER_NCS = m_analyticModel.GetServerUID ();
        FEED_NAME = m_analyticModel.GetFeedName ();

        // Install the TAKServer preference when first installed
        if (! pluginAlreadyInstalled) {
            // Connect to the TAK Server specified in the preference file.
            ImportPrefSort prefImporter = new ImportPrefSort (mapView.getContext(),
                                                              false, false);
            File prefFile = null;
            try {
                // String TMP_PREFERENCE_FILE = "/sdcard/atak/apolloedge/apollo_edge.pref";
                String TMP_PREFERENCE_FILE = getTmpFile ("", "apolloTakServer.pref");
                
                // Note: this file will get moved to ATAK's config directory at
                // /storage/emulated/0/atak/config/prefs/ after installation
                prefFile = m_analyticModel.createPrefFile (TMP_PREFERENCE_FILE);
                prefImporter.beginImport (prefFile);
                Log.d (TAG, "Added/updated profile for TAK Server");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d (TAG, "Unable to Add / update TAKServer");
            }
        }

        
        // Setup to watch TAK Server notifying mission changes:
        CommsMapComponent.getInstance().addOnCotEventListener (m_cotEventListner);
        
        // Setup to watch server connect/disconnect
        /*
         * This listener is used to detect server connectivity.  It gets
         * called whenever TAK server is connected or disconnected.  Once
         * the server is connected, we immediately subscribe to the
         * mission feed
         */
        m_outputsChangedListener =
            new CotServiceRemote.OutputsChangedListener() {
                @Override
                public void onCotOutputRemoved(Bundle descBundle) {
                    Log.d(TAG, "stream removed");
                }
                
                @Override
                public void onCotOutputUpdated(Bundle descBundle) {
                    Log.d (TAG, "got onCotOutputUpdated");
                    String serverNcs = descBundle.getString
                        (CotPort.CONNECT_STRING_KEY);
                    if (! serverNcs.equals (SERVER_NCS)) {
                        Log.d(TAG, "Skip other server " + serverNcs);
                        return;
                    }
                    
                    Log.d(TAG,
                          "Received message for "
                          + descBundle.getString(CotPort.DESCRIPTION_KEY)
                          + " and "
                          + descBundle.getString(CotPort.CONNECT_STRING_KEY)
                          + ": enabled="
                          + descBundle.getBoolean(CotPort.ENABLED_KEY, true)
                          + ": connected="
                          + descBundle.getBoolean(CotPort.CONNECTED_KEY, false));
                    boolean connected_flg = descBundle.getBoolean (CotPort.CONNECTED_KEY, false);
                    Log.d(TAG, "set connected_flg (TAKServer is connecte/reachable?) = " +
                          connected_flg);
                    boolean enabled_flg = descBundle.getBoolean (CotPort.ENABLED_KEY, true);
                    Log.d(TAG, "set enabled_flg (TAKServer is enabled/allowed??) = " + enabled_flg);
                    
                    if (enabled_flg && connected_flg) {
                        // check to see if mission feed is already subscribed
                        // feedManager.getSubscribedFeeds();
                        // Test to see if feed already subscribed:
                        Log.d (TAG, "subscribed feeds are : " + m_feedManager.getSubscribedFeeds());
                        List<Feed> feedList = m_feedManager.getSubscribedFeeds();
                        for (Feed feedElm : feedList) {
                            if (feedElm.getName().equals(FEED_NAME)) {
                                Log.d (TAG, "Checking already subscribed feed for update: " +
                                       FEED_NAME);

                                // check for updated feed content
                                m_restManager.executeRequest (m_getFeedReq, m_feedReqListener);
                                return;
                            }
                        }
                        
                        Log.d (TAG, "*** Server connected no feed. Need to subscribed to feed " +
                               FEED_NAME);
                        SubscribeAndDownload (SERVER_NCS, FEED_NAME);
                    }
                }
            };
        CommsMapComponent.getInstance().addOutputsChangedListener(m_outputsChangedListener);

        
        /* we initialize mission feed stuff here */
        // **** Setup Mission Feed:  ****
        setupMissionFeed (getMapView());

        
        // Create instance
        getInstance ();
    }


    /**
     * Distroy the Data Feed instance.
     * Must explictily call this function when unloading the plugin.
     */
    public static void dispose() {
        Log.d(TAG, "calling destroying DataFeed..");
        MissionAPI.dispose ();

        // Unload Mission Feed stuff
        CommsMapComponent.getInstance().removeOnCotEventListener(m_cotEventListner);
        CommsMapComponent.getInstance().removeOutputsChangedListener(m_outputsChangedListener);

        // m_pluginContext.unregisterReceiver (m_analyticStarterReceiver);

        // Register the broadcast receiver(s)
        try {
            Log.d(TAG, "unregisterReceiver analyticstarterbroadcastreceiver again");
            m_pluginContext.unregisterReceiver (m_analyticStarterReceiver);
        } catch (Exception e) {
            Log.d (TAG, Log.getStackTraceString(e));
        }
        
        // Sync hash from disk in case out of sync...
        m_analyticModel.syncHashFromDisk ();
        
        Log.d(TAG, "done destroying DataFeed");
    }


    /**
     * Obtain the Data Feed instance.
     */
    public static DataFeedAPI getInstance() {
        // All instances share the same static variables.
        // an instance is needed to make a function call.
        return m_instance != null ? m_instance : (m_instance = new DataFeedAPI());
    }


    // public static AnalyticModelData getAnalyticModel() {
    //     return  m_analyticModel;
    // }
    

    /**
     * Setup data/mission feed.
     * Create a TAK Server profile and connect to it.  Once connected,
     * immediately subscribe to the mission feed.
     */
    private static void setupMissionFeed (MapView mapView) {
        Log.d (TAG, "CAlling setupMissionFeed");
        
        // check to see if we already have server in the preference profile.
        TAKServer takServer = m_takServerListener.findServer (SERVER_NCS);
        Log.d (TAG, "checking takServer " + takServer);
        // if (takServer == null || !takServer.isConnected())
        //     {
        //         // Connect to the TAK Server specified in the preference file.
        //         ImportPrefSort prefImporter = new ImportPrefSort(mapView.getContext(),
        //                                                          false, false);
        //         File prefFile = null;
        //         try {
        //             // String TMP_PREFERENCE_FILE = "/sdcard/atak/apolloedge/apollo_edge.pref";
        //             String TMP_PREFERENCE_FILE = getTmpFile ("", "takServer.pref");
                    
        //             // Note: this file will get moved to ATAK's config directory at
        //             // /storage/emulated/0/atak/config/prefs/ after installation
        //             prefFile = m_analyticModel.createPrefFile (TMP_PREFERENCE_FILE);
        //         } catch (IOException e) {
        //             e.printStackTrace();
        //         }
        //         prefImporter.beginImport (prefFile);
        //         Log.d (TAG, "Added/updated profile for " + takServer);
        //     }

        // Register to the mission feed ASAP, but only if the server is up.
        final CotMapComponent inst = CotMapComponent.getInstance();
        // CotPortListActivity.CotPort[] servers = inst.getServers();
        // Log.d (TAG, "servers = " + servers);
        Log.d (TAG, "inst.isServerConnected() = " + inst.isServerConnected());
        if (! inst.isServerConnected())
            return;
        // at least one server is connected
        Boolean enabled_flg = false;
        Boolean connected_flg = false;
        Bundle b = CommsMapComponent.getInstance().getAllPortsBundle();
        Bundle[] streams = (Bundle[]) b.getParcelableArray("streams");
        Log.d (TAG, "looking for server " + SERVER_NCS);
        for (Bundle stream : streams) {
            Log.d(TAG, "stream " + stream.getString(CotPort.DESCRIPTION_KEY)
                  + ": " + stream.getString(CotPort.CONNECT_STRING_KEY)
                  + ": " + stream.getBoolean(CotPort.ENABLED_KEY)
                  + ": " + stream.getBoolean(CotPort.CONNECTED_KEY));
            if (stream.getString(CotPort.CONNECT_STRING_KEY).equals (SERVER_NCS)) {
                Log.d (TAG, "Found server " + SERVER_NCS);
                enabled_flg = stream.getBoolean(CotPort.ENABLED_KEY);
                connected_flg = stream.getBoolean(CotPort.CONNECTED_KEY);
            }
        }
        Log.d (TAG, "connected_flg = " + connected_flg);
        Log.d (TAG, "enabled_flg = " + enabled_flg);
        
        if (enabled_flg && connected_flg)
            SubscribeAndDownload (SERVER_NCS, FEED_NAME);
    }


    /**
     * Verify activity is in the config file.
     * @param activity The activity name (eg., MyActivityClass).
     * @return True if activity is defined in the config file.  False otherwise.
     */
    public static boolean HasActivity(String activity) {
        Log.d (TAG, "checking activity " + activity);
        return m_analyticModel.HasActivity (activity);
    }


    /**
     * Start the activity.
     * @param activity The activity name (eg., MyActivityClass).
     */
    public static void StartActivity(String activity) {
        if (! HasActivity (activity)) {
            Log.d (TAG, "ERROR, activity not defined in config file " + activity);
            return;
        }
        String analytic = m_analyticModel.GetAnalyticFromActivity (activity);
        Log.d (TAG, "Calling StartActivity, analytic = " + analytic + " activity = " + activity);
        
        // Send activity to activity starter broadcast to avoid race condition
        Intent intent = new Intent();
        intent.setAction (ANALYTIC_STARTER_BROADCAST);
        intent.putExtra ("action", "START_ANALYTIC");
        intent.putExtra ("activity", activity);
        intent.putExtra ("analytic", analytic);
        Log.d (TAG, "Calling sendBroadcast");
        m_pluginContext.sendBroadcast (intent);
    }
    

    /**
     * @hidden
     */
    public static void UpdateModelHash (String analytic, String fileName, String newHash) {
        Log.d (TAG, "calling UpdateModelHash");
        try {
            m_analyticModel.UpdateHash (analytic, fileName, newHash);
            m_analyticModel.Save (null);
        } catch (FileNotFoundException e) {
            Log.d (TAG, "Error updating hash config file");
            e.printStackTrace();
        }
    }

    
    /*******************************
     *      Helper Functions       *
     *******************************/

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
    
    private static String getTmpFile (String rootDir, String tmpFileName) {
        File appDir = m_pluginContext.getExternalFilesDir (null);
        if (null == appDir) {
            Log.d (TAG, "cannot get external files dir, " +
                   "external storage state is " + Environment.getExternalStorageState());

            return null;
        }
        
        File externalDir = new File(appDir, rootDir); // TC 2021-03-15 (Mon) --
        String ROOT = externalDir.getAbsolutePath();
        Log.d (TAG, "temp file = " + ROOT + File.separator + tmpFileName);
        return ROOT + File.separator + tmpFileName;
    }
        
    /* Copies relative to the application internal directory
     * (eg.,"/internal/storage/Android/data/com.your_app_name/files/{rootDir}/{filename}")
     */
    private static String copyAsset (AssetManager mgr, String rootDir, String filename) {
        InputStream in = null;
        OutputStream out = null;

        File appDir = m_pluginContext.getExternalFilesDir (null);
        if (null == appDir) {
            Log.d (TAG, "cannot get external files dir, " +
                   "external storage state is " + Environment.getExternalStorageState());

            return null;
        }
        
        File externalDir = new File(appDir, rootDir); // TC 2021-03-15 (Mon) --
        String ROOT = externalDir.getAbsolutePath();
        File file;
        try {
            Log.d (TAG, "copyAsset checking " + ROOT + File.separator + filename);
            file = new File(ROOT + File.separator + filename);
            if (!file.exists()) {
                file.createNewFile();
            }

            in = mgr.open (filename);
            out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1){
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Log.d(TAG, sw.toString());
            
            Log.d (TAG,"1copyAsset error!");
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    Log.d(TAG, sw.toString());
                    Log.d (TAG,"2copyAsset error!");
                    return null;
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // LOGGER.e(e, "IOExcetion!");
                    e.printStackTrace();                
                    Log.d (TAG,"3copyAsset error!");
                    return null;
                }
            }
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    
    /*
     * Subscribe to the mission feed and start
     * downloading model files specified in APOLLO_MODEL_CONFIG_FILE.
     */
    private static void SubscribeAndDownload(String serverNCS, String name) {
        //
        // Subscribe to a feed (mission)
        Log.d(TAG, "\n\n");
        Log.d(TAG, "----------------");
        Log.d(TAG, "Calling SubscribeAndDownload, SERVER_NCS=" + SERVER_NCS);
        Log.d(TAG, "----------------");

        TAKServer takServer = m_takServerListener.findServer (SERVER_NCS);
        if (takServer == null)
            {
                Log.d(TAG, "TakServer not found");
                return;
            }
        Log.d(TAG, "setting connected = true? " + takServer.isConnected());
        Log.d(TAG, "takServer =" + takServer);
        
        GetFeedsListRequest feedListReq = new GetFeedsListRequest (takServer);
        feedListRequestListenerClass feedListReqListener = new feedListRequestListenerClass();
        m_restManager.executeRequest (feedListReq, feedListReqListener);
    }

    
    /*
     * This function download a file in the mission feed if needed.
     * If so, it then notifies the analytic of the new model.
     *
     * Note, the file is downloaded using enterprise sycn. example location:
     * /storage/emulated/0/atak/tools/datasync/192.168.1.167-8080-tcp-..
     */
    private static void CheckDownloadFile(File downloadedFILE) {
        // A new file was downloaded now, we check the version.
        String fileName = downloadedFILE.getName();
        assert (fileName != null);
        String analytic = m_analyticModel.GetAnalytic (fileName);
        String activity = m_analyticModel.GetActivity (fileName);
        if (analytic == null) {
            // toast("Don't care file = " + fileName);
            Log.d (TAG, "Don't care file = " + fileName);
            return;
        }
        
        // We can silenly update model if analytic is not already running:
        Log.d (TAG, "Matched analytic " + analytic + " for file " + fileName);
        
        // Check hash values to see if different.
        String srcFilePath = downloadedFILE.getAbsolutePath();
        String destFilePath = m_analyticModel.GetPath (fileName);
        String newHash = GetNewHash (srcFilePath);
        String oldHash = m_analyticModel.GetHash (fileName);
        assert (srcFilePath != null && destFilePath != null &&
                newHash != null && oldHash != null);
        
        Log.d (TAG, "oldHash = " + oldHash);
        Log.d (TAG, "newHash = " + newHash);
        if (newHash.equals (oldHash)) {
            Log.d (TAG, "**Model same hash value, No need to replace model!");
            return;
        }
        
        // recentActivity.notify (missionFeed.uid,
        //                        "Feed Event", "A updated model file is available : " +
        //                        analytic + ":" + fileName);
        // toast (""Apollo " + analytic + " has an updated model file : " +
        //        fileName);
        
        // Send MISSION_UPDATE broadcast
        Intent intent = new Intent();
        intent.setAction (ANALYTIC_STARTER_BROADCAST);
        intent.putExtra ("action", "MISSION_UPDATED");
        intent.putExtra ("feedUid", m_missionFeed.uid);
        intent.putExtra ("analytic", analytic);
        intent.putExtra ("activity", activity);
        intent.putExtra ("fileName", fileName);
        intent.putExtra ("srcFilePath", srcFilePath);
        intent.putExtra ("destFilePath", destFilePath);
        intent.putExtra ("oldHash", oldHash);
        intent.putExtra ("newHash", newHash);
        Log.d (TAG, "Calling MISSION_UPDATED sendBroadcast, intent = " + intent);
        m_pluginContext.sendBroadcast (intent);
    }
    

    private static String GetNewHash(String filePath) {
        // filePath is the location of mission feed
        // ie. atak/tools/datasync/192.168.1.167-8080-tcp-apollo/<hash>/data_sync_load_failure.png
        Log.d(TAG, "filePath = " + filePath);
                
        String[] nameParts = filePath.split ("/", 0);
        Log.d(TAG, "length is " + nameParts.length);
        assert (nameParts.length > 2);
        return nameParts[nameParts.length - 2];

    }

}
    
