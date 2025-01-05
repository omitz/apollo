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

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * HTTP REST association files with a mission
 */
public class PutFeedFilesMetadataRequest extends AbstractRequest {

    public static final String TAG = "PutFeedFilesMetadataRequest";

    private List<FeedFile> _files;

    public PutFeedFilesMetadataRequest(Feed feed, List<FeedFile> files) {
        super(feed);
        _files = files;
    }

    public PutFeedFilesMetadataRequest(JSONObject json)
            throws JSONException {
        super(json);
        JSONData array = new JSONData(json.getJSONArray("Files"), true);
        _files = array.getList("Files", getFeed(), FeedFile.class);
    }

    public boolean isValid() {
        return super.isValid() && !FileSystemUtils.isEmpty(_files);
    }

    public List<FeedFile> getContents() {
        return _files;
    }

    private boolean hasContents() {
        return !FileSystemUtils.isEmpty(_files);
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED_FILES_METADATA;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(
                R.string.mn_failed_to_publish_file_metadata,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            JSONArray fileArray = new JSONArray();
            json.put("Files", fileArray);
            if (hasContents()) {
                for (FeedFile s : _files)
                    fileArray.put(s.toJSON(true).toJSON());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }
}
