package com.caci.apollo.datafeed;

import com.atakmap.coremap.log.Log;
import com.atakmap.filesystem.HashingUtils;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
    {
        "modelsInfo": [
            {
                "analytic": "SpeakerID", 
                "activity": "speakerIDActivity",   // must match the activity name exactly
                "dataPackages": [
                    {
                        "name": "unique_speakerid_data_package_name1.zip",
                    },
                    {
                        "name": "unique_spakerid_data_package_name2.zip",
                    }
                ]
            },
            {
                "analytic": "WebView",
                "activity": "webViewActivity",    // must match the activity name exactly
                "dataPackages": [
                    {
                        "name": "unique_webview_data_package_name1.zip",
                    },
                    {
                        "name": "unique_webview_data_package_name2.zip",
                    }
                ]
            }
        ]
    }
 */

public final class AnalyticModelData {
    public static final String TAG = "Tommy AnalyticModelData";
    static String ATAK_PACKAGE_PATH = "/sdcard/atak/tools/datapackage/";
    
    Map<String, FileInfoClass> m_fileToInfoMap = new HashMap<>(); 
    Map<String, String> m_activityToAnalytic = new HashMap<>();
    LinkedHashMap<String, AnalyticInfoClass> m_analyticsInfos = // given analytic get all
        new LinkedHashMap<String, AnalyticInfoClass>(10);       // of its info
    String m_serverUID;
    String m_feedName;
    String m_absConfigPath;
    
    class AnalyticInfoClass {
        String analytic;
        String activity;
        LinkedHashMap<String,ModelFileInfoClass>  modelFileInfos = // given file in analytic
            new LinkedHashMap<String,ModelFileInfoClass>(10);      // lookup more of its detail

        class ModelFileInfoClass {
            String name;
            String path;
            String hash;
        }
        
        public AnalyticInfoClass (String _analytic, String _activity)
        {
            analytic = _analytic;
            activity = _activity;
        }

        void addModelFile (String fileName, String filePath, String fileHash) {
            ModelFileInfoClass modelFileInfo = new ModelFileInfoClass();
            modelFileInfo.name = fileName;
            modelFileInfo.path = filePath;
            modelFileInfo.hash = fileHash;
            this.modelFileInfos.put (fileName, modelFileInfo);
        };

        void setModelFileHash (String fileName, String hash) {
            ModelFileInfoClass modelFileInfo =
                this.modelFileInfos.get (fileName);
            modelFileInfo.hash = hash;
            Log.d (TAG, "Set " + fileName + " hash to " + hash);
            Log.d (TAG, "Get back hash as: " + GetHash (fileName));
        }

        String getModelFileHash (String fileName) {
            ModelFileInfoClass modelFileInfo =
                this.modelFileInfos.get (fileName);
            return (modelFileInfo.hash);
        }


        LinkedHashMap getRecord () {
            LinkedHashMap analyticRecord = new LinkedHashMap(10);
            analyticRecord.put("analytic", analytic); // eg "SpeakerID"
            analyticRecord.put("activity", activity); // eg "speakerIDActivity"
            JSONArray dataPackagesArray = new JSONArray();
            
            for (ModelFileInfoClass modelFileInfo : this.modelFileInfos.values()) {
                LinkedHashMap fileRecord = AddFileInfo
                    (modelFileInfo.name, modelFileInfo.path, modelFileInfo.hash);
                dataPackagesArray.add (fileRecord);
            }
            
            analyticRecord.put("dataPackages", dataPackagesArray);
            return analyticRecord;
        }

        void syncHashFromDisk () {
            Boolean needSync_flg = false;
            for (ModelFileInfoClass modelFileInfo : this.modelFileInfos.values()) {
                Log.d (TAG, "syncHashFromDisk for " + modelFileInfo.path);
                try {
                    File f = new File (modelFileInfo.path);
                    String hash = HashingUtils.sha256sum(f);
                    if (! modelFileInfo.hash.equals (hash)) {
                        Log.d (TAG, "!!Inconsistent hash found for " + modelFileInfo.path);
                        Log.d (TAG, "!! modelFileInfo.hash = " + modelFileInfo.hash);
                        Log.d (TAG, "!! hash = " + hash);
                        modelFileInfo.hash = hash;
                        needSync_flg = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d (TAG, "Error computing disk hash for " + modelFileInfo.path);
                }
            }
            if (needSync_flg) {
                try {
                    Save (null);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    

    class FileInfoClass {
        String analytic;
        String activity;
        String path;
        // String hash;
        
        public FileInfoClass (String _analytic, String _activity, String _path)
        {
            analytic = _analytic;
            activity = _activity; // simple name, not the class name
            path = _path;
            // hash = _hash;
        }
    };


    private LinkedHashMap AddFileInfo (String name, String path, String hash) {
        LinkedHashMap n = new LinkedHashMap(10);
        n.put("name", name);
        n.put("path", path);
        n.put("hash", hash);
        return n;
    }


    private LinkedHashMap CreateSpeakerIDAnalytic () {
        LinkedHashMap analyticRecord = new LinkedHashMap(10);
        analyticRecord.put("analytic", "SpeakerID");
        analyticRecord.put("activity", "speakerIDActivity");
        JSONArray dataPackagesArray = new JSONArray();

        // file 1
        LinkedHashMap fileRecord = AddFileInfo ("fileName1.txt", "/sdcard/Download/", "12345");
        dataPackagesArray.add (fileRecord);

        // file 2
        fileRecord = AddFileInfo ("fileName2.txt", "/sdcard/Download/", "12345");
        dataPackagesArray.add (fileRecord);

        analyticRecord.put("dataPackages", dataPackagesArray);
        return analyticRecord;
    }


    private LinkedHashMap CreateWebViewAnalytic () {
        LinkedHashMap analyticRecord = new LinkedHashMap(10);
        analyticRecord.put("analytic", "WebView");
        analyticRecord.put("activity", "webViewActivity");
        JSONArray dataPackagesArray = new JSONArray();

        // file 1
        LinkedHashMap fileRecord = AddFileInfo ("fileNameA.txt", "/sdcard/Download/", "123_45");
        dataPackagesArray.add (fileRecord);

        // file 2
        fileRecord = AddFileInfo ("fileNameB.txt", "/sdcard/Download/", "123_45");
        dataPackagesArray.add (fileRecord);

        // file 3
        fileRecord = AddFileInfo ("fileNameC.txt", "/sdcard/Download/", "123_45");
        dataPackagesArray.add (fileRecord);
        
        analyticRecord.put("dataPackages", dataPackagesArray);
        return analyticRecord;
    }

    
    void TestWrite() throws IOException, JSONException {

        Log.d (TAG, "Calling Person Write");
        
        //creating JSONObject 
        JSONObject jo = new JSONObject();

        // for modelIfno, first create JSONArray
        Map analyticRecord;
        JSONArray analyticArray = new JSONArray();
        
        // Add speakerID:
        analyticRecord = CreateSpeakerIDAnalytic ();
        analyticArray.add (analyticRecord);

        // Add webView:
        analyticRecord = CreateWebViewAnalytic ();
        analyticArray.add (analyticRecord);
        
        // putting analytics to JSONObject
        jo.put("modelsInfo", analyticArray);

        // writing JSON to file:"JSONExample.json" in cwd
        PrintWriter pw = new PrintWriter("/sdcard/Download/JSONExample.json");
        pw.write(jo.toString());

        pw.flush();
        pw.close();
    }


    public String GetServerUID () {
        return m_serverUID;
    }

    public String GetFeedName () {
        return m_feedName;
    }

    
    public void Read(String absConfigPath) { // eg "/sdcard/Download/JSONExample.json"
        // parsing file "JSONExample.json" 

        Log.d (TAG, "Calling AnalyticModelData Read, file = " + absConfigPath);

        Object obj = null;
        try {
            obj = new JSONParser().parse(new FileReader(absConfigPath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // typecasting obj to JSONObject 
        JSONObject jo = (JSONObject) obj;

        // get server and feed info
        m_serverUID = (String) jo.get("serverUID");
        m_feedName = (String) jo.get("feedName");

        
        // getting analytics array 
        JSONArray analyticArray = (JSONArray) jo.get("modelsInfo");
            if (analyticArray == null) {
                Log.d(TAG, "** ERROR reading config file, could not find 'modelsInfo' ");
            }

        
        Log.d(TAG, "len = " + analyticArray.size());
        for (int a_idx = 0; a_idx < analyticArray.size(); a_idx++) {
            JSONObject analytic1 = (JSONObject) analyticArray.get(a_idx);
            String analytic = (String) analytic1.get("analytic");
            String activity = (String) analytic1.get("activity");

            Log.d(TAG, "analytic = " + analytic);
            Log.d(TAG, "activity = " + activity);
            AnalyticInfoClass analyticInfo = new AnalyticInfoClass(analytic, activity);
            assert (! m_activityToAnalytic.containsKey (activity));
            m_activityToAnalytic.put (activity, analytic);
            assert (m_activityToAnalytic.containsKey (activity));
            HasActivity (activity);
                
            JSONArray dataPackages = (JSONArray) analytic1.get("dataPackages");
            if (dataPackages == null) {
                Log.d(TAG, "** ERROR reading config file, could not find 'dataPackages' ");
            }
            Log.d(TAG, "len = " + dataPackages.size());
            
            for (int f_idx = 0; f_idx < dataPackages.size(); f_idx++) {
                JSONObject fileInfo = (JSONObject) dataPackages.get(f_idx);
                String fileName = (String) fileInfo.get("name");
                String filePath = ATAK_PACKAGE_PATH + fileName; // everything is a data package
                Log.d(TAG, "fileName = " + fileName);
                Log.d(TAG, "filePath = " + filePath);
                String fileHash = (String) fileInfo.get("hash");
                if (fileHash == null)
                    fileHash = "n/a";
                Log.d(TAG, "fileHash = " + fileHash);
                
                // Maintain mapping from file to info
                FileInfoClass fileInfoObj = new FileInfoClass (analytic, activity, filePath);
                m_fileToInfoMap.put (fileName, fileInfoObj);

                // 
                analyticInfo.addModelFile (fileName, filePath, fileHash);
            }
            m_analyticsInfos.put (analytic, analyticInfo);
        }

        m_absConfigPath = absConfigPath;
    }

    public File createPrefFile (String fileName) throws IOException {

        Log.d (TAG, "Writing preference file to " + fileName);
        
        String prefContent = String.format
            ("<?xml version='1.0' standalone='yes'?>%n" +
             "<preferences>%n" +
             "  <preference version=\"1\" name=\"cot_streams\">%n" +
             "    <entry key=\"count\" class=\"class java.lang.Integer\">2</entry>%n" +
             "    <entry key=\"description0\" class=\"class java.lang.String\">Demo Mission Feed TAK Server</entry>%n" + 
             "    <entry key=\"enabled0\" class=\"class java.lang.Boolean\">true</entry>%n" +
             "    <entry key=\"connectString0\" class=\"class java.lang.String\">%s</entry>%n" + 
             // "    <entry key=\"description1\" class=\"class java.lang.String\">Tommy's Local Server</entry>%n" +
             // "    <entry key=\"enabled1\" class=\"class java.lang.Boolean\">true</entry>%n" +
             // "    <entry key=\"connectString1\" class=\"class java.lang.String\">192.168.1.167:8088:tcp</entry>%n" +
             "  </preference>%n" +
             "  <preference version=\"1\" name=\"com.atakmap.app_preferences\">%n" +
             "    <entry key=\"displayServerConnectionWidget\" class=\"class java.lang.Boolean\">true</entry>%n" +
             "  </preference>%n" +
             "</preferences>", m_serverUID);

        FileWriter prefFile = new FileWriter (fileName);
        prefFile.write (prefContent);
        prefFile.close ();
        
        return new File (fileName);
    }

    public void syncHashFromDisk () {
        // actually compute hash from file on disk
        Log.d (TAG, "Calling syncHashFromDisk...");
        for (AnalyticInfoClass analyticsInfos : m_analyticsInfos.values()) {
            analyticsInfos.syncHashFromDisk ();
        }
    }

    // public String ComputeHash (File f) {
    //     return HashingUtils.sha256sum(f);
    // }
    
    public String GetAnalytic (String fileName) {
        // Return example:  WebViewer
        if (m_fileToInfoMap.containsKey (fileName)) {
            FileInfoClass fileInfoObj = m_fileToInfoMap.get (fileName);
            return fileInfoObj.analytic;
        }

        // fileName does not belong to any known analytic
        return null;
    }

    public String GetAnalyticFromActivity(String activity) {
        if (m_activityToAnalytic.containsKey (activity)) {
            return m_activityToAnalytic.get (activity);
        }
        return null;
    }

    public boolean HasActivity (String activity) {
        Log.d (TAG, "Calling HasActivity for " + activity);
        Log.d(TAG, "m_activityToAnalytic = " + m_activityToAnalytic.size());

        if (m_activityToAnalytic.containsKey (activity)) {
            Log.d (TAG, "HasActivity = true");
            return true;
        }
        Log.d (TAG, "HasActivity = false");
        return false;
    }

    public String GetActivity (String fileName) {
        // Return example:  com.atakmap.android.testMissionAPI.WebViewActivity
        if (m_fileToInfoMap.containsKey (fileName)) {
            FileInfoClass fileInfoObj = m_fileToInfoMap.get (fileName);
            return fileInfoObj.activity;
        }
        // fileName does not belong to any known analytic
        return null;
    }

    public String GetPath (String fileName) {
        // Return example:  "/sdcard/Download/README.md"
        if (m_fileToInfoMap.containsKey (fileName)) {
            FileInfoClass fileInfoObj = m_fileToInfoMap.get (fileName);
            return fileInfoObj.path;
        }

        // fileName does not belong to any known analytic
        return null;
    }


    public String GetHash (String fileName) {
        // Return example:  asdfasf --- generated by sha256
        String analytic = GetAnalytic (fileName);
        if (analytic != null) {
            AnalyticInfoClass analyticInfo = m_analyticsInfos.get(analytic);
            return analyticInfo.getModelFileHash (fileName);
        }
        return null;
    }

    void UpdateHash (String analytic, String fileName, String hash) throws FileNotFoundException {
        //
        AnalyticInfoClass analyticInfo = m_analyticsInfos.get(analytic);
        if (analyticInfo != null) 
            analyticInfo.setModelFileHash (fileName, hash);
    }

    void Save (String absConfigPath) throws FileNotFoundException {
        // Save back the changes,eg "/sdcard/Download/JSONExample_v2.json"

        //creating JSONObject 
        JSONObject jo = new JSONObject();

        jo.put("serverUID", m_serverUID);
        jo.put("feedName", m_feedName);

        
        // for modelIfno, first create JSONArray
        JSONArray analyticArray = new JSONArray();

        // Add analytics
        for (AnalyticInfoClass analyticsInfos : m_analyticsInfos.values()) {
            analyticArray.add (analyticsInfos.getRecord ());
        }

        // putting analytics to JSONObject
        jo.put("modelsInfo", analyticArray);

        // writing JSON to file:"JSONExample.json" in cwd
        if (absConfigPath == null)
            absConfigPath = m_absConfigPath;
        Log.d (TAG, "writing to absConfigPath = " + absConfigPath);
        PrintWriter pw = new PrintWriter(absConfigPath);
        pw.write(jo.toString());

        pw.flush();
        pw.close();
    }
    
}

