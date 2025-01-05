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
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Set/change feed password
 */
public class PutFeedPasswordRequest extends AbstractRequest {

    private static final String TAG = "PutFeedPasswordRequest";

    private final String _password;

    public PutFeedPasswordRequest(Feed feed, String password) {
        super(feed);
        _password = password;
    }

    public PutFeedPasswordRequest(JSONObject json) throws JSONException {
        super(json);
        _password = json.has("Password") ? json.getString("Password") : null;
    }

    public boolean hasPassword() {
        return !FileSystemUtils.isEmpty(_password);
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("password");
        sb.addParam("password", _password);
        sb.addParam("creatorUid", getCreatorUID());
        return sb.toString();
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED_PASSWORD;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_set_feed_password,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            if (hasPassword())
                json.put("Password", _password);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }
}
