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

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get all users subscribed to a mission
 */
public class GetFeedSubsRequest extends AbstractRequest {

    private static final String TAG = "GetFeedSubsRequest";

    private boolean _roles;

    public GetFeedSubsRequest(Feed feed, boolean roles) {
        super(feed);
        _roles = roles;
    }

    public GetFeedSubsRequest(JSONObject json) throws JSONException {
        super(json);
        _roles = json.has("Roles") && json.getBoolean("Roles");
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("subscriptions");
        if (_roles)
            sb.addPath("roles");
        return sb.toString();
    }

    @Override
    public String getMatcher() {
        return RestManager.MATCHER_MISSION_SUB;
    }

    @Override
    public int getRequestType() {
        return RestManager.GET_FEED_SUBSCRIPTIONS;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_get_users,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            if (_roles)
                json.put("Roles", true);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
