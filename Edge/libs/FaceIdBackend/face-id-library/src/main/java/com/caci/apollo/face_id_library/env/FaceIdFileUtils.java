// package pp.facerecognizer.env;
package com.caci.apollo.face_id_library.env;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class FaceIdFileUtils {
    // private static final Logger LOGGER = new Logger();
    private static String TAG = "Tommy FaceIdFileUtils";
    // public static final String ROOT =
    //     Environment.getExternalStorageDirectory().getAbsolutePath() +
    //     File.separator + "facerecognizer"; // /storage/emulated/0/facerecognizer
    // private static String ROOT =  Environment.getExternalStorageDirectory().getAbsolutePath() +
    //     File.separator + "facerecognizer"; // /storage/emulated/0/facerecognizer
    private static String ROOT;

    // public static final String SYNC_DIR = "syncFaceID"; 
    public static final String DATA_FILE = "data";
    // public static final String PROFILE_FILE = "profiles.zip";
    public static final String PROFILE_FILE = "profiles";
    public static final String MODEL_FILE = "model";
    public static final String LABEL_FILE = "label";


    public static void setRoot(Context context, String rootDir,
                               boolean useAbsolutePath) throws IOException {
        // ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() +
        //     File.separator + rootDir; // /storage/emulated/0/facerecognizer

        // Make it consistant to speakerID.
        // /storage/emulated/0/storage/Android/data/com.your_app_name/files/
        // "/internal/storage/Android/data/com.your_app_name/files/"

        if (useAbsolutePath) {
            ROOT = rootDir;
        }
        else {
            
            File appDir = context.getExternalFilesDir(null);
            if (null == appDir)
                throw new IOException("cannot get external files dir, "
                                      + "external storage state is " +
                                      Environment.getExternalStorageState());
            
            File externalDir = new File(appDir, rootDir); // TC 2021-03-15 (Mon) --
            ROOT = externalDir.getAbsolutePath();
        }

        Log.d (TAG, "setRoot to " + ROOT);
    }

    public static String getRoot () {
        return ROOT;
    }

    public static String getModelPath() {
        return ROOT + File.separator + MODEL_FILE;
    }

    public static String getDataPath() {
        return ROOT + File.separator + DATA_FILE;
    }
    
    public static String getProfilePath() {
        return ROOT + File.separator + PROFILE_FILE;
    }
    
    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     * @param absFilename The absolute file path to save the bitmap to.
     */
    public static void saveBitmap(final Bitmap bitmap, final String absFilename) {
        // LOGGER.i("Saving %dx%d bitmap to %s.", bitmap.getWidth(), bitmap.getHeight(), ROOT);
        // Log.d(TAG, String.format ("Saving %dx%d bitmap to %s.",
        //                           bitmap.getWidth(), bitmap.getHeight(), ROOT));
        // final File myDir = new File(ROOT);

        // if (!myDir.isDirectory()) {
        //     if (!myDir.mkdirs()) {
        //         // LOGGER.i("Make dir failed");
        //         Log.d(TAG, "Error Make dir failed");
        //         return;
        //     }
        // }

        // final File file = new File (myDir, filename);
        Log.d(TAG, String.format ("Saving %dx%d bitmap to %s.",
                                  bitmap.getWidth(), bitmap.getHeight(), absFilename));
        final File file = new File (absFilename);
        if (file.exists()) {
            file.delete();
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress (Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            // LOGGER.e(e, "Exception!");
            e.printStackTrace();                
            Log.d (TAG,"SavBitmap error!");
            return;
        }
    }


    public static boolean copyAsset(AssetManager mgr, String filename) {
        InputStream in = null;
        OutputStream out = null;

        try {
            Log.d (TAG, "copyAsset checking " + ROOT + File.separator + filename);
            File file = new File(ROOT + File.separator + filename);
            if (!file.exists()) {
                file.createNewFile();
            }

            in = mgr.open (filename);
            out = new FileOutputStream (file);

            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1){
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            // LOGGER.e(e, "Excetion!");
            // e.printStackTrace();

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Log.d(TAG, sw.toString());
            
            Log.d (TAG,"1copyAsset error!");
            return false;            
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // LOGGER.e(e, "IOExcetion!");
                    // e.printStackTrace();                
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    Log.d(TAG, sw.toString());
                    Log.d (TAG,"2copyAsset error!");
                    return false;
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // LOGGER.e(e, "IOExcetion!");
                    e.printStackTrace();                
                    Log.d (TAG,"3copyAsset error!");
                    return false;
                }
            }
        }
        return true;
    }

    public static void appendText(String text, String filename) {
        try(FileWriter fw = new FileWriter(ROOT + File.separator + filename, true);
            PrintWriter out = new PrintWriter(new BufferedWriter(fw))) {
            out.println(text);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
            // LOGGER.e(e, "IOException!");
            e.printStackTrace();                
            Log.d (TAG,"appendText error!");
            return;
        }
    }

    public static ArrayList<String> readLabel(String filename) throws FileNotFoundException{

        String path = ROOT + File.separator + filename;
        Log.d (TAG, "label path is " + path);
        Scanner s = new Scanner(new File(path));
        ArrayList<String> list = new ArrayList<>();
        while (s.hasNextLine()){
            list.add(s.nextLine());
        }
        s.close();

        return list;
    }


    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
}    
}
