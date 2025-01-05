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

import com.atakmap.android.missionapi.data.AuthManager;
import com.atakmap.android.missionapi.data.TAKServerVersion;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.http.OpType;
import com.atakmap.android.missionapi.net.http.put.PutFeedInviteRequest;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * HTTP REST delete feed invite
 * Same endpoint as the PUT request
 */
public class DeleteFeedInviteRequest extends PutFeedInviteRequest {

    private static final String TAG = "DeleteFeedInviteRequest";

    private final String _inviteToken;

    public DeleteFeedInviteRequest(Feed feed, String callsign) {
        super(feed, callsign);
        _inviteToken = AuthManager.getInviteToken(feed);
    }

    public DeleteFeedInviteRequest(Feed feed) {
        super(feed, MapView.getDeviceUid(), null, null);
        _inviteToken = AuthManager.getInviteToken(feed);
    }

    public DeleteFeedInviteRequest(JSONObject json) throws JSONException {
        super(json);
        _inviteToken = json.has("InviteToken") ? json.getString("InviteToken")
                : null;
    }

    @Override
    public OpType getOperationType() {
        return OpType.DELETE;
    }

    private boolean hasInviteToken() {
        return !FileSystemUtils.isEmpty(_inviteToken);
    }

    @Override
    public String getToken() {
        return hasInviteToken() ? _inviteToken : super.getToken();
    }

    @Override
    public boolean checkSupport(TAKServerVersion supportVersion) {
        // Deletion uses non-bulk request scheme
        return supportVersion != TAKServerVersion.SUPPORT_BULK_INVITE
                && super.checkSupport(supportVersion);
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_dismiss_invitation,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            if (hasInviteToken())
                json.put("InviteToken", _inviteToken);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
