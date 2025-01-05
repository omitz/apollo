package apollo_utils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ApolloFileUtils {

    public static void performFileSearch(Activity activity, int requestCode) {
        /*
        requestCode: the index of the person (according to the classifier) - 1
         */
        // create an intent with which you will start an activity
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // get a result from the activity
        activity.startActivityForResult(intent, requestCode);
    }

    public static void copyAsset(AssetManager mgr, String srcFilename, String destFilepath) {
        InputStream in = null;
        OutputStream out = null;

        try {
            File destFile = new File(destFilepath);
            if (!destFile.exists()) {
                destFile.createNewFile();
            }

            in = mgr.open(srcFilename);
            out = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1){
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String idExtension(String filename) {
        // We would use MimeTypeMap.getFileExtensionFromUrl(filename); but that function behaves unexpectedly when there is a + in the filename, and we currently use a + sometimes when indicating time zone
        int dotPos = filename.lastIndexOf('.');
        String fileExt = null;
        if (0 <= dotPos) {
            fileExt = filename.substring(dotPos + 1);
        }
        return fileExt;
    }

    public static String filenameFromPath(String fullpath) {
        String[] parts = fullpath.split(File.separator);
        String filename = parts[parts.length - 1];
        return filename;
    }

    public static String extensionFromFullpath(String fullpath) {
        String filename = filenameFromPath(fullpath);
        String fileExt = idExtension(filename);
        return fileExt;
    }
}