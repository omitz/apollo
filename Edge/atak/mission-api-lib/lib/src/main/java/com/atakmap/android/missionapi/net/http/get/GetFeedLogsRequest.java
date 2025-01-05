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
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

/**
 * Get user logs associated with a feed
 */
public class GetFeedLogsRequest extends AbstractRequest {

    private static final String TAG = "GetFeedLogsRequest";

    private long _lastSyncTime;

    public GetFeedLogsRequest(Feed feed, long lastSyncTime) {
        super(feed);
        _lastSyncTime = lastSyncTime;
    }

    public GetFeedLogsRequest(JSONObject json) throws JSONException {
        super(json);
        _lastSyncTime = json.getLong("LastSyncTime");
    }

    public long getLastSyncTime() {
        return _lastSyncTime;
    }
    
    @Override
    public int getRequestType() {
        return RestManager.GET_FEED_LOGS;
    }
    
    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("log");
        if (_lastSyncTime > 0) {
            long secAgo = (long) Math.ceil((new CoordinatedTime()
                    .getMilliseconds() - _lastSyncTime) / 1000d);
            sb.addParam("secago", secAgo);
        }
        return sb.toString();
    }

    @Override
    public String getMatcher() {
        return RestManager.MATCHER_LOGENTRY;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_get_logs,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("LastSyncTime", _lastSyncTime);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
