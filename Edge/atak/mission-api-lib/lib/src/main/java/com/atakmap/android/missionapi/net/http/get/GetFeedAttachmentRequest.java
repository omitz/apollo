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

package com.atakmap.android.missionapi.net.http.get;

import org.json.JSONException;
import org.json.JSONObject;

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.RetryRequest;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

/**
 * Created by byoung on 4/12/2016.
 * TODO does not fully implement Parcelable. As we use JSON for its serialization
 */
public class GetFeedAttachmentRequest extends RetryRequest {

    private static final String TAG = "GetFeedAttachmentRequest";
    
    private final String _hash;

    /**
     * UID of the marker or user log this file is attached to
     */
    private final String _refUID;

    private final String _destDir;

    public GetFeedAttachmentRequest(Feed feed,
                                    String hash, String refUID, String desinationDir) {
        super(feed, 0);
        _hash = hash;
        _refUID = refUID;
        _destDir = desinationDir;
    }

    public GetFeedAttachmentRequest(JSONObject json)
            throws JSONException {
        super(json);
        _hash = json.getString("Hash");
        _refUID = json.getString("RefUID");
        _destDir = json.getString("DestinationDir");
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
                !FileSystemUtils.isEmpty(_hash) &&
                !FileSystemUtils.isEmpty(_refUID) &&
                !FileSystemUtils.isEmpty(_destDir);
    }

    @Override
    public String getContentUID() {
        return getHash();
    }

    public String getHash() {
        return _hash;
    }

    public String getRefUID() {
        return _refUID;
    }

    public String getDestinationDir() {
        return _destDir;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder("api/sync/search");
        sb.addParam("hash", getHash());
        return sb.toString();
    }

    @Override
    public String getMatcher() {
        return RestManager.MATCHER_RESOURCE;
    }

    @Override
    public int getRequestType() {
        return RestManager.GET_FEED_ATTACHMENT;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_get_attachments,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("Hash", _hash);
            json.put("RefUID", _refUID);
            json.put("DestinationDir", _destDir);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }
}
