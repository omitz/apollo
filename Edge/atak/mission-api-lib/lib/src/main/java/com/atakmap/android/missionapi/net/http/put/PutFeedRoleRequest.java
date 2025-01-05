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
import com.atakmap.android.missionapi.model.json.user.UserRole;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Change user role
 */
public class PutFeedRoleRequest extends AbstractRequest {

    private static final String TAG = "PutFeedRoleRequest";

    private final String _clientUid;
    private final UserRole _role;

    public PutFeedRoleRequest(Feed feed, String clientUid, UserRole role) {
        super(feed);
        _clientUid = clientUid;
        _role = role;
    }

    public PutFeedRoleRequest(JSONObject json) throws JSONException {
        super(json);
        _clientUid = json.getString("ClientUID");
        _role = UserRole.fromString(json.getString("Role"));
    }

    public String getClientUID() {
        return _clientUid;
    }

    public UserRole getRole() {
        return _role;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("role");
        sb.addParam("clientUid", _clientUid);
        sb.addParam("role", _role.toServerString());
        return sb.toString();
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED_ROLE;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_change_role,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("ClientUID", _clientUid);
            json.put("Role", _role.name());
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }
}
