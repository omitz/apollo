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
 * HTTP REST delete feed
 */
public class DeleteFeedRequest extends AbstractRequest {

    private static final String TAG = "DeleteFeedRequest";

    private boolean _deepDelete;

    public DeleteFeedRequest(Feed feed, boolean deepDelete) {
        super(feed);
        _deepDelete = deepDelete;
    }

    public DeleteFeedRequest(JSONObject json) throws JSONException {
        super(json);
        _deepDelete = json.has("DeepDelete");
    }

    @Override
    public int getRequestType() {
        return RestManager.DELETE_FEED;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_delete_feed,
                getFeedName());
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addParam("creatorUid", getCreatorUID());
        if (_deepDelete)
            sb.addParam("deepDelete", true);
        return sb.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            if (_deepDelete)
                json.put("DeepDelete", true);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }
}
