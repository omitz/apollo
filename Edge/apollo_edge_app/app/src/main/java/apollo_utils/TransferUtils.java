package apollo_utils;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class TransferUtils {

    public static String get_api_root_url() {
//        For testing using your local flask-apollo-processor and analytics, connect the phone and computer to the same wifi and use your computer's IP address assigned by your router
//        String apiRootUrl = "http://yourIPAddress:8080/";
        String apiRootUrl = "https://api.apollo-cttso.com/";
        return apiRootUrl;
    }

    public static void queueUploadViaApi(Activity activity, String fullLocalFilepath, String s3inputDir) {
        if (fullLocalFilepath == null) {
            Toast.makeText(activity, "Could not find the filepath of the selected file",
                    Toast.LENGTH_LONG).show();
            return;
        }
        File file = new File(fullLocalFilepath);
        String s3filePath = s3inputDir + file.getName();
        String uploadMsg = String.format("Queuing up %s to %s", fullLocalFilepath, s3filePath);
        Log.i("TransferUtils", uploadMsg);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, uploadMsg,
                        Toast.LENGTH_LONG).show();
            }
        });
        URL url = null;
        try {
            String urlStr = TransferUtils.get_api_root_url() + "upload/";
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // Upload to S3
        new NetworkTask("PUT", fullLocalFilepath, null).execute(url);
    }
}
