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

package com.atakmap.android.missionapi.net.offline;

import com.atakmap.android.missionapi.data.FeedManager;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedChange;
import com.atakmap.android.missionapi.model.json.FeedCoTChange;
import com.atakmap.android.missionapi.model.json.FeedExternalChange;
import com.atakmap.android.missionapi.model.json.FeedFileChange;
import com.atakmap.android.missionapi.model.json.FeedLogChange;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;

/**
 * Represents an offline request to be fulfilled once the connection has
 * been re-established
 */
public class OfflineRequest {

    private static final String TAG = "OfflineRequest";

    // Sorted by timestamps descending
    // Newest at first index -> oldest at last index
    public static final Comparator<OfflineRequest> TIME_SORT
            = new Comparator<OfflineRequest>() {
        @Override
        public int compare(OfflineRequest lhs, OfflineRequest rhs) {
            return Long.compare(rhs.time, lhs.time);
        }
    };

    // Content types
    public enum ContentType {
        COT, FILE, EXTERNAL, LOG, MISSION, UNKNOWN
    }

    // Request UID (mission UID + content UID)
    public final String uid;

    // Content UID
    public final String contentUID;

    // Server connect string
    public final String server;

    // Mission UID
    public final String missionUID;

    // Timestamp when this request was made
    public final long time;

    // The mission's last sync time in milliseconds when this request was made
    // Used for conflict checking
    public final long lastSync;

    // Content type
    public final ContentType contentType;

    // Content title (if applicable)
    public final String contentTitle;

    // Raw content representation (CoT event, file path, etc.)
    public final String contentRaw;

    // Change data JSON representation
    public final FeedChange change;

    public OfflineRequest(String server, String missionUID, long time,
                          long lastSync, ContentType contentType, String contentUID,
                          String contentTitle, String contentRaw, FeedChange change) {
        this.server = server;
        this.missionUID = missionUID;
        this.time = time;
        this.lastSync = lastSync;
        this.contentType = contentType;
        this.contentTitle = contentTitle;
        this.contentUID = contentUID;
        this.contentRaw = contentRaw == null ? "" : contentRaw;
        this.change = change;
        this.uid = createUID();
    }

    public OfflineRequest(Feed feed, long time,
                          ContentType contentType, String contentUID, String contentTitle,
                          String contentRaw, FeedChange change) {
        this(feed.serverNCS, feed.uid, time,
                feed.getLastSyncTime(), contentType, contentUID,
                contentTitle, contentRaw, change);
    }

    public OfflineRequest(String server, String feedUID, long time,
                          long syncTime, ContentType contentType, String contentUID,
                          String contentTitle, String contentRaw, String changeData) {
        this(server, feedUID, time, syncTime, contentType, contentUID,
                contentTitle, contentRaw, fromChangeJSON(feedUID, changeData));
    }

    public OfflineRequest(Feed feed, ContentType contentType,
                          String contentUID, String contentTitle, String contentRaw,
                          FeedChange change) {
        this(feed, new CoordinatedTime().getMilliseconds(), contentType,
                contentUID, contentTitle, contentRaw, change);
    }

    public OfflineRequest(Feed feed, FeedChange change, String contentRaw) {
        this.server = feed.serverNCS;
        this.missionUID = feed.uid;
        this.change = change;
        this.time = change.getTime();
        this.lastSync = feed.getLastSyncTime();
        this.contentRaw = contentRaw == null ? "" : contentRaw;
        this.contentTitle = change.getContentTitle();
        this.contentUID = change.getContentUID();

        if (change instanceof FeedCoTChange)
            this.contentType = ContentType.COT;
        else if (change instanceof FeedFileChange)
            this.contentType = ContentType.FILE;
        else if (change instanceof FeedExternalChange)
            this.contentType = ContentType.EXTERNAL;
        else if (change instanceof FeedLogChange)
            this.contentType = ContentType.LOG;
        else {
            this.contentType = ContentType.UNKNOWN;
            Log.e(TAG, "Invalid change: " + change, new Throwable());
        }

        this.uid = createUID();
    }

    public OfflineRequest(Feed feed, JSONObject json) throws JSONException {
        this(json.getString("server"), json.getString("missionUID"),
                json.getLong("time"), json.getLong("lastSync"),
                ContentType.valueOf(json.getString("contentType")),
                json.getString("contentUID"), json.getString("contentTitle"),
                json.getString("contentRaw"), FeedChange.fromJSON(feed,
                        new JSONData(json.get("change"), true)));
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("server", this.server);
            json.put("missionUID", this.missionUID);
            json.put("time", this.time);
            json.put("lastSync", this.lastSync);
            json.put("contentType", this.contentType.name());
            json.put("contentUID", this.contentUID);
            json.put("contentTitle", this.contentTitle);
            json.put("contentRaw", this.contentRaw);
            json.put("change", this.change.toJSON(true));
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON: " + this, e);
        }
        return json;
    }

    public boolean isValid() {
        // Title and request JSON are optional
        return !FileSystemUtils.isEmpty(this.uid)
                && !FileSystemUtils.isEmpty(this.server)
                && !FileSystemUtils.isEmpty(this.missionUID)
                && this.time > 0
                && this.contentType != null
                && !FileSystemUtils.isEmpty(this.contentUID)
                && this.change != null;
    }

    @Override
    public String toString() {
        return TAG + "(" + this.change.type + ", " + this.contentType
                + ", " + this.contentTitle + ", " + this.uid + ")";
    }

    private String createUID() {
        return createUID(this.missionUID, this.contentUID);
    }

    public String getChangeJSON() {
        if (this.change == null)
            return "";
        try {
            return this.change.toJSON(true).toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get change JSON: " + this, e);
        }
        return "";
    }

    public static String createUID(String missionUID, String contentUID) {
        return missionUID + "." + contentUID;
    }

    private static FeedChange fromChangeJSON(String feedUID, String changeData) {
        try {
            Feed feed = FeedManager.getInstance().getFeed(feedUID);
            if (feed != null)
                return FeedChange.fromJSON(feed, new JSONData(changeData, true));
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert from JSON: " + changeData);
        }
        return null;
    }
}
