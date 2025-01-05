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
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.offline.OfflineRequest;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * HTTP REST put data package
 */
public class PutOfflinePackageRequest extends PutDataPackageRequest {

    private static final String TAG = "PutOfflinePackageRequest";

    private Set<String> _requestUIDs = new HashSet<>();

    public PutOfflinePackageRequest(Feed feed, File zip,
            List<OfflineRequest> requests) {
        super(feed, zip.getAbsolutePath(), "");
        for (OfflineRequest req : requests)
            _requestUIDs.add(req.uid);
    }

    public PutOfflinePackageRequest(JSONObject json) throws JSONException {
        super(json);
        if (json.has("RequestUIDs")) {
            JSONArray arr = json.getJSONArray("RequestUIDs");
            for (int i = 0; i < arr.length(); i++)
                _requestUIDs.add(arr.getString(i));
        }
    }

    public Set<String> getRequestUIDs() {
        return _requestUIDs;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            if (!_requestUIDs.isEmpty()) {
                JSONArray arr = new JSONArray();
                for (String uid : _requestUIDs)
                    arr.put(uid);
                json.put("RequestUIDs", arr);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_OFFLINE_PACKAGE;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_publish_content,
                getFeedName());
    }
}
