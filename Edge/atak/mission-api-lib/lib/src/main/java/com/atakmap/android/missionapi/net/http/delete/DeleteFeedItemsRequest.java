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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

/**
 * HTTP REST delete feed items
 */
public class DeleteFeedItemsRequest extends AbstractRequest {

    private static final String TAG = "DeleteFeedItemsRequest";

    private final List<String> _uids;

    public DeleteFeedItemsRequest(Feed feed, List<String> uids) {
        super(feed);
        _uids = uids;
    }

    public DeleteFeedItemsRequest(JSONObject json) throws JSONException {
        super(json);
        _uids = new ArrayList<>();
        if (json.has("UIDs")) {
            JSONArray arr = json.getJSONArray("UIDs");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++)
                    _uids.add(arr.getString(i));
            }
        }
    }

    @Override
    public boolean isValid() {
        return super.isValid() && !FileSystemUtils.isEmpty(_uids);
    }

    public List<String> getUIDs() {
        return _uids;
    }

    private boolean hasUIDs() {
        return !FileSystemUtils.isEmpty(_uids);
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public int getRequestType() {
        return RestManager.DELETE_FEED_ITEMS;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("contents");
        sb.addParam("uid", args[0]);
        sb.addParam("creatorUid", getCreatorUID());
        return sb.toString();
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_remove_items,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            JSONArray uidArray = new JSONArray();
            json.put("UIDs", uidArray);
            if (hasUIDs()) {
                for (String s : _uids)
                    uidArray.put(s);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }
}
