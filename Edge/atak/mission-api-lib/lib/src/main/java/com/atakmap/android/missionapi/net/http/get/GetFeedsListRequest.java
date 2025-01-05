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

import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.data.TAKServerVersion;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.comms.TAKServer;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get the full list of feeds on the server
 */
public class GetFeedsListRequest extends AbstractRequest {

    public static final String TAG = "GetFeedsListRequest";

    private String _tool = "public";
    private boolean _includePasswords = true;

    public GetFeedsListRequest(String serverNCS) {
        super(serverNCS, "all", serverNCS);
    }

    public GetFeedsListRequest(TAKServer server) {
        this(server.getConnectString());
    }

    public GetFeedsListRequest(TAKServer server, String tool) {
        this(server);
        this._tool = tool;
    }

    public GetFeedsListRequest(JSONObject json) throws JSONException {
        super(json);
        JSONData data = new JSONData(json, true);
        _tool = data.get("tool");
    }

    /**
     * Set whether this request should return feeds that have password protection
     * @param incPass True to include feeds that have password protection
     */
    public void includePasswordFeeds(boolean incPass) {
        _includePasswords = incPass;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder("api/missions");
        if (_includePasswords && checkSupport(TAKServerVersion.SUPPORT_AUTH))
            sb.addParam("passwordProtected", true);
        if (checkSupport(TAKServerVersion.SUPPORT_ROLES))
            sb.addParam("defaultRole", true);
        sb.addParam("tool", _tool);
        return sb.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("tool", _tool);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON");
        }
        return json;
    }

    @Override
    public String getMatcher() {
        return RestManager.MATCHER_MISSION;
    }

    @Override
    public int getRequestType() {
        return RestManager.GET_FEEDS;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_get_feeds,
                getServerURL());
    }
}
