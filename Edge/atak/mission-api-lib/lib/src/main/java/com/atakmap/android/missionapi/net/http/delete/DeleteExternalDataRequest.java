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
import com.atakmap.android.missionapi.model.json.FeedExternalData;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;

/**
 * HTTP REST delete external data
 */
public class DeleteExternalDataRequest extends DeleteFeedItemsRequest {

    private static final String TAG = "DeleteExternalDataRequest";

    private final String _title;
    private final String _notes;

    public DeleteExternalDataRequest(Feed feed, FeedExternalData data,
                                     String notes) {
        super(feed, Collections.singletonList(data.getUID()));
        _title = data.getTitle();
        _notes = notes;
    }

    public DeleteExternalDataRequest(JSONObject json) throws JSONException {
        super(json);
        _title = json.getString("Title");
        _notes = json.getString("Notes");
    }

    public String getNotes() {
        return _notes;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(getMissionPath());
        sb.addPath("externaldata");
        sb.addPath(args[0]);
        sb.addParam("creatorUid", getCreatorUID());
        sb.addParam("notes", getNotes());
        return sb.toString();
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_remove_external,
                _title, getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject o = super.toJSON();
        try {
            o.put("Title", _title);
            o.put("Notes", _notes);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return o;
    }
}
