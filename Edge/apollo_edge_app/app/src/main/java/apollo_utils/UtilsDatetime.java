package apollo_utils;

import android.app.Activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UtilsDatetime {

    public static String createDatetimeStr(Activity activity) {
        Date curTime = Calendar.getInstance().getTime();
        Locale curLocale = activity.getResources().getConfiguration().getLocales().get(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ssZ", curLocale);
        String formattedDate = dateFormat.format(curTime);
        return formattedDate;
    }
}
