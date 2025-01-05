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

package com.atakmap.android.missionapi.net.http.put;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedLog;
import com.atakmap.android.missionapi.model.json.HashResourceFile;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

/**
 * HTTP REST put feed logs
 */
public class PutFeedLogsRequest extends AbstractRequest {

    private static final String TAG = "PutFeedLogsRequest";

    private final List<FeedLog> _logs;
    private final List<HashResourceFile> _files;

    public PutFeedLogsRequest(Feed feed, List<FeedLog> logs,
                              List<HashResourceFile> files) {
        super(feed);
        _logs = logs;
        _files = files;
    }

    public PutFeedLogsRequest(JSONObject json) throws JSONException {
        super(json);

        _logs = new ArrayList<>();
        if (json.has("Logs")) {
            Feed feed = getFeed();
            JSONArray arr = json.getJSONArray("Logs");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                FeedLog log = Feed.createLog(feed, new JSONData(o, true));
                if (log.isValid())
                    _logs.add(log);
            }
        }

        _files = new ArrayList<>();
        if (json.has("Files")) {
            JSONArray arr = json.getJSONArray("Files");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                HashResourceFile rf = HashResourceFile.fromJSON(o);
                if (rf.isValid())
                    _files.add(rf);
            }
        }
    }

    @Override
    public boolean isValid() {
        return super.isValid() && !FileSystemUtils.isEmpty(_logs);
    }

    public List<FeedLog> getLogs() {
        return _logs;
    }

    public List<HashResourceFile> getFiles() {
        return _files;
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED_LOGS;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_publish_logs,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            JSONArray arr = new JSONArray();
            json.put("Logs", arr);
            if (!FileSystemUtils.isEmpty(_logs)) {
                for (FeedLog s : _logs) {
                    // Serialize for non-server since we use 'serverTime'
                    // to determine if the log is being updated
                    arr.put(s.toJSON(false).toJSON());
                }
            }
            arr = new JSONArray();
            json.put("Files", arr);
            if (!FileSystemUtils.isEmpty(_files)) {
                for (HashResourceFile s : _files)
                    arr.put(s.toJSON());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
