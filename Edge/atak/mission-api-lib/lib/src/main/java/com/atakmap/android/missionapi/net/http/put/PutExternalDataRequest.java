/*
 * Copyright 2021 PAR Government Systems
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

import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedExternalDetails;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * HTTP REST request for publishing an external data reference to a feed
 */
public class PutExternalDataRequest extends AbstractRequest {

    private static final String TAG = "PutExternalDataRequest";

    private final FeedExternalDetails _details;

    public PutExternalDataRequest(Feed feed, FeedExternalDetails details) {
        super(feed);
        _details = details;
    }

    public PutExternalDataRequest(JSONObject json) throws JSONException {
        super(json);
        _details = new FeedExternalDetails(new JSONData(
                json.getJSONObject("Details"), true));
    }

    @Override
    public boolean isValid() {
        return super.isValid() && _details != null;
    }

    public FeedExternalDetails getDetails() {
        return _details;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("externaldata");
        sb.addParam("creatorUid", getCreatorUID());
        return sb.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("Details", _details.toJSON(true).toJSON());
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_EXTERNAL_DATA;
    }

    @Override
    public String getRequestBody() {
        return _details.toJSON(true).toString();
    }

    @Override
    public String getMatcher() {
        return "ExternalMissionData";
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(
                R.string.mn_failed_to_publish_external_data,
                getFeedName());
    }
}
