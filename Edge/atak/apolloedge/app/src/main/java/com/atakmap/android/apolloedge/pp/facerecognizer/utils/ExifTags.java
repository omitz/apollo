package com.atakmap.android.apolloedge.pp.facerecognizer.utils;

public class ExifTags {
    public String TAG_GPS_LATITUDE;
    public String TAG_GPS_LATITUDE_REF;
    public String TAG_GPS_LONGITUDE;
    public String TAG_GPS_LONGITUDE_REF;

    // Constructor
    public ExifTags(String latitude, String latRef, String longitude, String longRef) {
        this.TAG_GPS_LATITUDE = latitude;
        this.TAG_GPS_LATITUDE_REF = latRef;
        this.TAG_GPS_LONGITUDE = longitude;
        this.TAG_GPS_LONGITUDE_REF = longRef;
    }
}