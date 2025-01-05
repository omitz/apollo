package com.atakmap.android.apolloedge.pp.facerecognizer.utils;

public class GeolocUtils {

    public static double convertDMSToDegree(String stringDMS) {
        String[] DMS = stringDMS.split(",", 3);
        double deg = divide(DMS[0]);
        double min = divide(DMS[1]);
        double sec = divide(DMS[2]);
        double result = deg + (min / 60) + (sec / 3600);
        return result;
    }

    /**
     * Convert lat or long in decimal degrees format to degrees minutes seconds format.
     * @param decimalDegrees eg 38/1,52/1,493427/10000
     * @return lat or long in DMS format
     */
    public static String convertDegreesToDMS(double decimalDegrees) {
        // Get rid of the lat/long ref, since it's already been noted
        decimalDegrees = Math.abs(decimalDegrees);
        // Convert
        int degrees = (int) Math.floor(decimalDegrees);
        int minutes = (int) Math.floor(60 * (decimalDegrees - degrees));
        double seconds = (3600 * (decimalDegrees - degrees)) - (60 * minutes);
        int secondsDenom = 10000;
        seconds = seconds * secondsDenom;
        int secondsInt = (int) Math.round(seconds);
        String degreesStr = degrees + "/1";
        String minutesStr = minutes + "/1";
        String secondsStr = secondsInt + "/10000";
        return degreesStr + "," + minutesStr + "," + secondsStr;
    }

    private static double divide(String str) {
        String[] strArr = str.split("/", 2);
        Double num = Double.valueOf(strArr[0]);
        Double den = Double.valueOf(strArr[1]);
        double floatRes = num/den;
        return floatRes;
    }
}
