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
import com.atakmap.android.missionapi.R;
import com.atakmap.android.http.rest.request.GetCotEventRequest;
import com.atakmap.android.http.rest.request.GetCotHistoryRequest;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Get all CoT events for a feed
 * May also be used to obtain a single CoT event
 */
public class GetFeedCotRequest extends AbstractRequest {

    private static final String TAG = "GetFeedCotRequest";

    private final long _startTime;
    private final List<String> _whiteList;

    public GetFeedCotRequest(Feed feed, long startTime,
                             List<String> whiteList) {
        super(feed);
        _startTime = startTime;
        _whiteList = whiteList;
    }

    public GetFeedCotRequest(Feed feed, String uid) {
        super(feed);
        _startTime = -1;
        _whiteList = Collections.singletonList(uid);
    }

    public GetFeedCotRequest(JSONObject json) throws JSONException {
        super(json);
        _startTime = json.getLong("StartTime");
        _whiteList = new ArrayList<>();
        if (json.has("WhiteList")) {
            JSONArray uidArray = json.getJSONArray("WhiteList");
            for (int i = 0; i < uidArray.length(); i++)
                _whiteList.add(uidArray.getString(i));
        }
    }

    public long getStartTime() {
        return _startTime;
    }

    public List<String> getWhiteList() {
        return _whiteList;
    }

    public boolean hasWhiteList() {
        return !FileSystemUtils.isEmpty(_whiteList);
    }

    public boolean hasSingleUID() {
        return _whiteList != null && _whiteList.size() == 1;
    }

    public String getUID() {
        return hasSingleUID() ? _whiteList.get(0) : null;
    }

    @Override
    public int getRequestType() {
        return RestManager.GET_FEED_COT;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        if (hasSingleUID())
            return "api/cot/xml/" + getUID();
        else
            return super.getRequestEndpoint() + "/cot";
    }

    @Override
    public String getMatcher() {
        return hasSingleUID() ? GetCotEventRequest.COT_MATCHER
                : GetCotHistoryRequest.COT_MATCHER;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_get_items,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("StartTime", _startTime);
            JSONArray uidArray = new JSONArray();
            json.put("WhiteList", uidArray);
            if (hasWhiteList()) {
                for (String s : _whiteList)
                    uidArray.put(s);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
