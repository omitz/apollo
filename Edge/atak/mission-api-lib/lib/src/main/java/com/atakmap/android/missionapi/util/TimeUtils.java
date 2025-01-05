/*
 * Copyright 2020 PAR Government Systems
 *
 * Unlimited Rights:
 * PAR Government retains ownership rights to this software.  The Government has Unlimited Rights
 * to use, modify, reproduce, release, perform, display, or disclose this
 * software as identified in the purchase order contract. Any
 * reproduction of computer software or portions thereof marked with this
 * legend must also reproduce the markings. Any person who has been provided
 * access to this software must be aware of the above restrictions.
 */

package com.atakmap.android.missionapi.util;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {

    private static final String TAG = "TimeUtils";

    private static final SimpleDateFormat TIMESTAMP_FORMAT
            = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            LocaleUtil.getCurrent());

    private static final SimpleDateFormat LIBERAL_TIMESTAMP_FORMAT
            = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
            LocaleUtil.getCurrent());

    private static final SimpleDateFormat FILE_TIMESTAMP_FORMAT
            = new SimpleDateFormat("yyyyMMdd_HHmmss", LocaleUtil.getCurrent());

    static {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TIMESTAMP_FORMAT.setTimeZone(utc);
        LIBERAL_TIMESTAMP_FORMAT.setTimeZone(utc);
        FILE_TIMESTAMP_FORMAT.setTimeZone(utc);
    }

    /**
     * Get the current timestamp
     * @return Timestamp string
     */
    public static String getTimestamp() {
        return getTimestamp(new CoordinatedTime().getMilliseconds());
    }

    /**
     * Convert milliseconds to timestamp format used by server
     * @param milliseconds UNIX timestamp in milliseconds
     * @return Timestamp string
     */
    public static String getTimestamp(long milliseconds, boolean incMillis) {
        Date d = new Date(milliseconds);
        synchronized (TIMESTAMP_FORMAT) {
            // SimpleDateFormat requires synchronization when formatting
            // or else weird things happen
            return incMillis ? TIMESTAMP_FORMAT.format(d)
                    : LIBERAL_TIMESTAMP_FORMAT.format(d);
        }
    }

    public static String getTimestamp(long milliseconds) {
        return getTimestamp(milliseconds, true);
    }

    /**
     * Get a timestamp suitable for use within a file name
     * @param milliseconds UNIX timestamp in milliseconds
     * @return File timestamp
     */
    public static String getFileTimestamp(long milliseconds) {
        Date d = new Date(milliseconds);
        synchronized (FILE_TIMESTAMP_FORMAT) {
            return FILE_TIMESTAMP_FORMAT.format(d);
        }
    }

    public static String getFileTimestamp() {
        return getFileTimestamp(new CoordinatedTime().getMilliseconds());
    }

    /**
     * Convert timestamp to date
     * @param timestamp Timestamp string
     * @return Date
     */
    public static Date parseTimestamp(String timestamp) {
        synchronized (TIMESTAMP_FORMAT) {
            try {
                return TIMESTAMP_FORMAT.parse(timestamp);
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse timestamp", e);

                try {
                    return LIBERAL_TIMESTAMP_FORMAT.parse(timestamp);
                } catch (Exception e2) {
                    Log.w(TAG, "Failed to parse liberal timestamp", e2);
                }
            }
        }
        return null;
    }

    /**
     * Convert timestamp to milliseconds
     * @param timestamp Timestamp string
     * @return Milliseconds since UNIX epoch
     */
    public static long parseTimestampMillis(String timestamp) {
        Date d;
        if (!FileSystemUtils.isEmpty(timestamp)
                && (d = parseTimestamp(timestamp)) != null)
            return d.getTime();
        return 0;
    }
}
