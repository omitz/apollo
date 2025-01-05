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

package com.atakmap.android.missionapi.net.http.delete;

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP REST delete feed files
 */
public class DeleteFeedFilesRequest extends AbstractRequest {

    private static final String TAG = "DeleteFeedFilesRequest";
    
    private final List<String> _hashes;
    private boolean _replacing;

    public DeleteFeedFilesRequest(Feed feed, List<String> uids,
                                  boolean replacing) {
        super(feed);
        _hashes = uids;
        _replacing = replacing;
    }

    public DeleteFeedFilesRequest(JSONObject json) throws JSONException {
        super(json);
        _hashes = new ArrayList<>();
        _replacing = json.getBoolean("Replacing");
        if (json.has("Hashes")) {
            JSONArray uidArray = json.getJSONArray("Hashes");
            for (int i = 0; i < uidArray.length(); i++)
                _hashes.add(uidArray.getString(i));
        }
    }

    public boolean isValid() {
        return super.isValid() && !FileSystemUtils.isEmpty(_hashes);
    }

    public List<String> getHashes() {
        return _hashes;
    }

    public boolean isReplacing() {
        return _replacing;
    }

    private boolean hasUIDs() {
        return !FileSystemUtils.isEmpty(_hashes);
    }

    @Override
    public int getRequestType() {
        return RestManager.DELETE_FEED_FILES;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("contents");
        sb.addParam("hash", args[0]);
        sb.addParam("creatorUid", getCreatorUID());
        return sb.toString();
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_remove_files,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            JSONArray uidArray = new JSONArray();
            json.put("Hashes", uidArray);
            json.put("Replacing", _replacing);
            if (hasUIDs()) {
                for (String s : _hashes)
                    uidArray.put(s);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }
}
