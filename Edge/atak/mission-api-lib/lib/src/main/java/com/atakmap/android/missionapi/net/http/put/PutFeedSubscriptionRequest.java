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

import org.json.JSONException;
import org.json.JSONObject;

import com.atakmap.android.missionapi.data.AuthManager;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

/**
 * HTTP REST put feed subscription based on local device UID
 * notifications will be sent via CoT streaming connection
 */
public class PutFeedSubscriptionRequest extends AbstractRequest {

    private static final String TAG = "PutFeedSubscriptionRequest";

    private final boolean _subscribe;
    private final boolean _persist;
    private final String _password;
    private final String _inviteToken;
    //private final long _lastSyncTime;

    public PutFeedSubscriptionRequest(Feed feed, boolean subscribe,
                                      boolean persist) {
        super(feed);
        _subscribe = subscribe;
        _persist = persist;
        _password = feed.passwordProtected ? AuthManager.getPassword(feed)
                : null;
        _inviteToken = AuthManager.getInviteToken(feed);
        //_lastSyncTime = feed.getLastSyncTime();
    }

    public PutFeedSubscriptionRequest(JSONObject json) throws JSONException {
        super(json);
        _subscribe = json.getBoolean("Subscribe");
        _persist = json.getBoolean("Persist");
        _password = json.has("Password") ? json.getString("Password") : null;
        _inviteToken = json.has("InviteToken") ? json.getString("InviteToken")
                : null;
        //_lastSyncTime = json.getLong("LastSyncTime");
    }

    public boolean isSubscribe() {
        return _subscribe;
    }

    public boolean isPersist() {
        return _persist;
    }

    public boolean hasPassword() {
        return !FileSystemUtils.isEmpty(_password);
    }

    public boolean hasInviteToken() {
        return !FileSystemUtils.isEmpty(_inviteToken);
    }

    @Override
    public String getToken() {
        return hasInviteToken() ? _inviteToken : super.getToken();
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED_SUBSCRIPTION;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("subscription");
        sb.addParam("password", _password);
        sb.addParam("uid", getCreatorUID());
        /*sb.addParam("changes", true);
        sb.addParam("logs", true);
        if (_lastSyncTime > 0) {
            long secAgo = (long) Math.ceil((new CoordinatedTime()
                    .getMilliseconds() - _lastSyncTime) / 1000d);
            sb.addParam("secago", secAgo);
        }*/
        return sb.toString();
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_subscribe,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("Subscribe", _subscribe);
            json.put("Persist", _persist);
            if (hasInviteToken())
                json.put("InviteToken", _inviteToken);
            else if (hasPassword())
                json.put("Password", _password);
            //json.put("LastSyncTime", _lastSyncTime);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
