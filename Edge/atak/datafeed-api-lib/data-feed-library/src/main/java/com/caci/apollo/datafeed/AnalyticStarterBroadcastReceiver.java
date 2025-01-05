package com.caci.apollo.datafeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.atakmap.android.importfiles.sort.ImportMissionPackageSort;
import com.atakmap.android.missionapi.notifications.RecentActivity;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.atakmap.android.maps.MapView.getMapView;
import static com.atakmap.android.missionapi.util.FeedUtils.toast;


public class AnalyticStarterBroadcastReceiver extends BroadcastReceiver {

    /*******************************
     *          Constants          *
     *******************************/
    private static final String TAG = "Tommy AnlytcStrtrBR";
    public static final String ANALYTIC_STARTER_BROADCAST =
        "com.atakmap.android.testMissionAPI.AnalyticStarter";


    /*******************************
     *      Member Variables       *
     *******************************/
    private static RecentActivity m_recentActivity = new RecentActivity
        (NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID(), "MyTitle", "MyText");

    private static Map<String, Boolean> m_activityRunningMap = new HashMap<>();
    private static Map<String, Boolean> m_isOldModelMap = new HashMap<>();
    private static Map<String, ArrayList<ModelCopyFileInfoClass>> m_modelFilesQueue  =
        new HashMap<>();
    private static DataFeedAPI m_dataFeedAP = DataFeedAPI.getInstance ();
    
    /*******************************
     *      Data Strucuture        *
     *******************************/
    public static class MapCompat {
        public static <K, V> V getOrDefault(@NonNull Map<K, V> map, K key, V defaultValue) {
            V v;
            return (((v = map.get(key)) != null) || map.containsKey(key))
                ? v
                : defaultValue;
        }
    }
    
    static class ModelCopyFileInfoClass {
        String srcFilePath;
        String destFilePath;
        String analytic;
        String fileName;
        String oldHash;
        String newHash;
        public ModelCopyFileInfoClass (String _srcFilePath,
                                       String _destFilePath,
                                       String _analytic,
                                       String _fileName,
                                       String _oldHash,
                                       String _newHash) {
            srcFilePath = _srcFilePath;
            destFilePath = _destFilePath;
            analytic = _analytic;
            fileName = _fileName;
            oldHash = _oldHash;
            newHash = _newHash;
        }
    }


    private static class Task extends AsyncTask<String, Integer, String> {

        private final PendingResult pendingResult;
        private final Intent intent;
        private final Context context;
        private final RecentActivity recentActivity;

        private Task(PendingResult pendingResult, Intent intent,
                     Context context, RecentActivity recentActivity) {
            Log.d (TAG, "intent = " + intent);
            this.pendingResult = pendingResult;
            this.intent = intent;
            this.context = context;
            this.recentActivity = recentActivity;
        }
        
        @Override
        protected String doInBackground(String... strings) {
            StringBuilder sb = new StringBuilder();
            sb.append("Action: " + intent.getAction() + "\n");
            sb.append("URI: " + intent.toUri(Intent.URI_INTENT_SCHEME).toString() + "\n");

            String log = sb.toString();
            Log.d(TAG, log);

            String action = intent.getStringExtra("action");
            Log.d (TAG, "Action = " + action);
            if (action.equals("MISSION_UPDATED")) {

                String srcFilePath = intent.getStringExtra ("srcFilePath");
                String destFilePath = intent.getStringExtra ("destFilePath");
                String analytic = intent.getStringExtra ("analytic");
                String activity = intent.getStringExtra ("activity");
                String fileName = intent.getStringExtra ("fileName");
                String feedUid = intent.getStringExtra ("feedUid");
                String oldHash = intent.getStringExtra ("oldHash");
                String newHash = intent.getStringExtra ("newHash");
                Log.d(TAG, "feedUid = " + feedUid);
                assert (srcFilePath != null && destFilePath != null && analytic != null &&
                        activity != null && fileName != null && feedUid != null &&
                        oldHash != null && newHash != null);

                m_isOldModelMap.put (activity, true);

                // Ask user to restart if analytic is already running.
                Log.d (TAG, "*** getting running for activity = " + activity);
                
                if (MapCompat.getOrDefault (m_activityRunningMap, activity, false)) {

                    // Accumulate into queue
                    ArrayList<ModelCopyFileInfoClass> fileCopyQueue =
                        MapCompat.getOrDefault (m_modelFilesQueue, activity,
                                                new ArrayList<ModelCopyFileInfoClass>());
                    ModelCopyFileInfoClass copyFileInfo = new ModelCopyFileInfoClass
                        (srcFilePath,  destFilePath, analytic, fileName, oldHash, newHash);
                    fileCopyQueue.add (copyFileInfo);
                    m_modelFilesQueue.put (activity, fileCopyQueue);
                    
                    Log.d(TAG, "Please quit analytic to have the latest model applied.");
                    toast("Please quit " +  analytic + " to have its model replaced");
                    recentActivity.notify (feedUid,
                                           "Feed Event",
                                           "Please quit " + analytic +
                                           " to have its model replaced.");
                }
                else {
                    // We can silently replace the model
                    Log.d(TAG, "Activity not running.. Silently replacing the model.");
                    
                    ModelCopyFileInfoClass fileCopy = new ModelCopyFileInfoClass
                        (srcFilePath,  destFilePath, analytic, fileName, oldHash, newHash);
                    // CopyModelFile (fileCopy);
                    InstallMissionPackage (fileCopy); // TBF -- every file is a mission package...

                    // update the hash value
                    Log.d (TAG, "UPDATE_MODEL_HASH to: " + newHash);
                    m_dataFeedAP.UpdateModelHash (analytic, fileName, newHash);
                    
                    // analytic can start without updating model
                    m_isOldModelMap.put (activity, false);
                    
                    // user notifiation:
                    toast ("Apollo " + analytic +
                           " has been updated with the latest model file : " + fileName);
                    
                }
                

            } else if (action.equals ("START_ANALYTIC")) {
                Log.d(TAG, "Got START_ANALYTIC");
                String analytic = intent.getStringExtra ("analytic");
                String activity = intent.getStringExtra ("activity");
                assert (analytic != null && activity != null);
                StartPluginActivity (activity);
                
            } else if (action.equals ("ANALYTIC_RUNNING")) {
                String activity = intent.getStringExtra ("activity");
                assert (activity != null);
                Log.d(TAG, "Got ANALYTIC_RUNNING for " + activity);
                m_activityRunningMap.put (activity, true);
                                        
            } else if (action.equals ("ANALYTIC_NOT_RUNNING")) { // when activity is destroyed
                String activity = intent.getStringExtra ("activity");
                assert (activity != null);
                Log.d(TAG, "Got ANALYTIC_NOT_RUNNING for " + activity);
                m_activityRunningMap.put (activity, false);

                /* good time to update the model */
                if (MapCompat.getOrDefault (m_isOldModelMap, activity, false)) {
                    Log.d(TAG, "Need to replace model for next start");

                        // Must replace the model file first, which maybe many files queued...
                        toast (activity + "has been updated with the latest model.");
                        ArrayList<ModelCopyFileInfoClass> fileCopyQueue =
                            MapCompat.getOrDefault (m_modelFilesQueue, activity,
                                                    new ArrayList<ModelCopyFileInfoClass>());
                        Log.d (TAG, "fileCopyQueue.length = " + fileCopyQueue.size());
                        for (ModelCopyFileInfoClass fileCopy : fileCopyQueue) {
                            // CopyModelFile (fileCopy);
                            InstallMissionPackage (fileCopy); // assume file is a data package...
                            // update hash 
                            m_dataFeedAP.UpdateModelHash (fileCopy.analytic,
                                                          fileCopy.fileName, fileCopy.newHash);

                        }

                        // clear the queue and the next analytic start
                        // does not need to update model.
                        m_modelFilesQueue.remove (activity);
                        m_isOldModelMap.put (activity, false);

                }
                
                
            } else {
                Log.d(TAG, "Unhandled action !!!! " + action );
                throw new AssertionError ("Unhandled action !!!! " + action);
            }

            
            return log;
        }
        
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // Must call finish() so the BroadcastReceiver can be recycled.
            pendingResult.finish();
        }

        void StartPluginActivity(String activity) {
            // The ATAK plugin activities has the following format:
            //     packageName = "com.atakmap.android.apolloedge.plugin"; 
            //     className = "com.atakmap.android.apolloedge." + activity;
            
            Log.d(TAG, "Starting Analytic activity" + activity);
            Intent intent = new Intent();
            // String packageName = "com.atakmap.android.apolloedge.plugin"; 
            // String className = "com.atakmap.android.apolloedge." + activity;
            String packageName = context.getPackageName();
            String className = packageName.replace ("plugin", activity);
            Log.d (TAG, "packageName = " + packageName);
            Log.d (TAG, "className = " + className);
            intent.setClassName(packageName, className);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this shows a new task
            context.startActivity(intent);
        }

        private  void copy(File src, File dst) throws IOException {
            Log.d(TAG, "calling copy\n");
            
            FileInputStream inStream = new FileInputStream(src);
            FileOutputStream outStream = new FileOutputStream(dst);
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inStream.close();
            outStream.close();
            Log.d(TAG, "done calling copy\n");

        }


        void InstallMissionPackage (ModelCopyFileInfoClass fileCopy) {
            // import mission package (zip file)  
            Log.d (TAG, "Importing mission package " + fileCopy.destFilePath);
            ImportMissionPackageSort.ImportMissionV1PackageSort importer =
                new ImportMissionPackageSort.ImportMissionV1PackageSort
                (getMapView().getContext(), true, // if the extension needs to be validated
                 false,  // if the file needs to be copied
                 true); // if the mission package is required to have a manifest
            File zipFile = new File (fileCopy.srcFilePath);
            importer.beginImport (zipFile);
            Log.d (TAG, "**mission package imported");
        }
        
        // void CopyModelFile (ModelCopyFileInfoClass fileCopy) {
        //     Log.d(TAG, "Calling CopyModelFile = " + fileCopy);
        //     Log.d(TAG, "newHash = " + fileCopy.newHash);
        //     Log.d(TAG, "oldHash = " + fileCopy.oldHash);
            
        //     Log.d(TAG, "srcFilePath = " + fileCopy.srcFilePath);
        //     Log.d(TAG, "destFilePath = " + fileCopy.destFilePath);

        //     File source = new File (fileCopy.srcFilePath);
        //     File dest = new File (fileCopy.destFilePath);
        //     try {
        //         copy (source, dest);
        //     } catch (IOException e) {
        //         e.printStackTrace();
        //         Log.d (TAG, "Failed");
        //     }
        // }
    }
    
    /*******************************
     *      Inherited Functions    *
     *******************************/
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d (TAG, "onReceive, intent = " + intent);

        final String action = intent.getAction();
        if (action == null)
            return;

        // Show drop-down
        switch (action) {
            case ANALYTIC_STARTER_BROADCAST:
                final PendingResult pendingResult = goAsync();
                Task asyncTask = new Task(pendingResult, intent, context, m_recentActivity);
                asyncTask.execute();
                break;
        }
    }
    
}
