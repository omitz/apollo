package apollo_utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

public class PermissionsUtils {
    public static boolean hasPermission(Activity activity, String[] permissionsList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String permission : permissionsList) {
                boolean granted = activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    allGranted = false;
                }
            }
            return allGranted;
        } else {
            return true;
        }
    }
}
