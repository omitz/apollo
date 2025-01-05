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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

/**
 * HTTP REST put feed files
 */
public class PutFeedFilesRequest extends AbstractRequest {

    public static final String TAG = "PutFeedFilesRequest";
    
    private List<ResourceFile> _files;

    public PutFeedFilesRequest(Feed feed, List<ResourceFile> files) {
        super(feed);
        _files = files;
    }

    public PutFeedFilesRequest(JSONObject json) throws JSONException {
        super(json);
        JSONArray array = json.getJSONArray("Files");
        if (array == null || array.length() < 1)
            throw new JSONException("No JSON files");

        _files = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            ResourceFile s = ResourceFile.fromJSON(array.getJSONObject(i));
            if (s.isValid())
                _files.add(s);
        }
    }

    public boolean isValid() {
        return super.isValid() && !FileSystemUtils.isEmpty(_files);
    }

    public List<ResourceFile> getFiles() {
        return _files;
    }

    private boolean hasFiles() {
        return !FileSystemUtils.isEmpty(_files);
    }

    @Override
    public String getMatcher() {
        return RestManager.MATCHER_MISSION;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED_FILES;
    }

    @Override
    public String getErrorMessage() {
        String files = StringUtils.getString(R.string.custom_files,
                _files.size());
        if (_files.size() == 1)
            files = new File(_files.get(0).getFilePath()).getName();
        return StringUtils.getString(R.string.mn_publishing_item_failed,
                files, getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            JSONArray fileArray = new JSONArray();
            json.put("Files", fileArray);
            if (hasFiles()) {
                for (ResourceFile s : _files) {
                    fileArray.put(s.toJSON());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }
}
