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
import com.atakmap.android.missionapi.model.json.FeedChange;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get the full list of feeds on the server
 */
public class GetFeedChangesRequest extends AbstractRequest {

    private static final String TAG = "GetFeedChangesRequest";

    private final long _lastSyncTime;
    private final boolean _processChanges;

    public GetFeedChangesRequest(Feed feed, long lastSyncTime,
                                 boolean processChanges) {
        super(feed);
        _lastSyncTime = lastSyncTime;
        _processChanges = processChanges;
    }

    public GetFeedChangesRequest(JSONObject json) throws JSONException {
        super(json);
        _lastSyncTime = json.getLong("LastSyncTime");
        _processChanges = json.getBoolean("ProcessChanges");
    }

    public boolean getProcessChanges() {
        return _processChanges;
    }

    public long getLastSyncTime() {
        return _lastSyncTime;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder path = new URIPathBuilder(super.getRequestEndpoint());
        path.addPath("changes");
        path.addParam("squashed", false);
        if (_lastSyncTime > 0) {
            long secAgo = (long) Math.ceil((new CoordinatedTime()
                    .getMilliseconds() - _lastSyncTime) / 1000d);
            path.addParam("secago", secAgo);
        }
        return path.toString();
    }

    @Override
    public String getMatcher() {
        return FeedChange.MISSION_CHANGE_MATCHER;
    }

    @Override
    public int getRequestType() {
        return RestManager.GET_FEED_CHANGES;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_get_changes,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("LastSyncTime", _lastSyncTime);
            json.put("ProcessChanges", _processChanges);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
