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

import com.atakmap.android.missionapi.data.AuthManager;
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
 * Get a feed's metadata
 */
public class GetFeedRequest extends AbstractRequest {

    private static final String TAG = "GetFeedRequest";

    private String _notifyType = "";

    // Post-request parameters for fetching changes and logs
    private boolean _deepQuery;
    private long _lastSyncTime;
    private boolean _processChanges;
    private String _password;

    public GetFeedRequest(Feed feed) {
        super(feed);
        if (!AuthManager.hasAccessToken(feed))
            _password = AuthManager.getPassword(feed);
    }

    public GetFeedRequest(JSONObject json) throws JSONException {
        super(json);
        _notifyType = json.getString("NotificationType");
        if (json.has("DeepQuery"))
            setDeepQuery(json.getLong("LastSyncTime"),
                    json.getBoolean("ProcessChanges"));
        if (json.has("Password"))
            _password = json.getString("Password");
    }

    public void setNotificationType(String type) {
        _notifyType = type;
    }

    public String getNotificationType() {
        return _notifyType;
    }

    public void setDeepQuery(long lastSyncTime, boolean processChanges) {
        _lastSyncTime = lastSyncTime;
        _processChanges = processChanges;
        _deepQuery = true;
    }

    public boolean isDeepQuery() {
        return _deepQuery;
    }

    public long getLastSyncTime() {
        return _lastSyncTime;
    }

    public boolean getProcessChanges() {
        return _processChanges;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        if (!FileSystemUtils.isEmpty(_password))
            sb.addParam("password", _password);
        /*if (isDeepQuery() && checkSupport(TAKServerVersion
                .SUPPORT_BULK_FEED_DATA)) {
            sb.addParam("changes", true);
            sb.addParam("logs", true);
            if (_lastSyncTime > 0) {
                long secAgo = (long) Math.ceil((new CoordinatedTime()
                        .getMilliseconds() - _lastSyncTime) / 1000d);
                sb.addParam("secago", secAgo);
            }
        }*/
        return sb.toString();
    }

    @Override
    public String getMatcher() {
        return RestManager.MATCHER_MISSION;
    }

    @Override
    public int getRequestType() {
        return RestManager.GET_FEED;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_get_feed,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("NotificationType", getNotificationType());
            if (isDeepQuery()) {
                json.put("DeepQuery", true);
                json.put("LastSyncTime", getLastSyncTime());
                json.put("ProcessChanges", getProcessChanges());
            }
            if (!FileSystemUtils.isEmpty(_password))
                json.put("Password", _password);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
