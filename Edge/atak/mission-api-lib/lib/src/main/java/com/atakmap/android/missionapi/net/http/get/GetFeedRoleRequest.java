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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get the user's current role
 */
public class GetFeedRoleRequest extends AbstractRequest {

    private static final String TAG = "GetFeedRoleRequest";

    public GetFeedRoleRequest(Feed feed) {
        super(feed);
    }

    public GetFeedRoleRequest(JSONObject json) throws JSONException {
        super(json);
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("role");
        return sb.toString();
    }

    @Override
    public String getMatcher() {
        return "MissionRole";
    }

    @Override
    public int getRequestType() {
        return RestManager.GET_FEED_ROLE;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_get_role,
                getFeedName());
    }
}
