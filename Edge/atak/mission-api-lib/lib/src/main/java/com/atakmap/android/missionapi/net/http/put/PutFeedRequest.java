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

import com.atakmap.android.missionapi.data.AuthManager;
import com.atakmap.android.missionapi.data.TAKServerVersion;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * HTTP REST delete feed
 */
public class PutFeedRequest extends AbstractRequest {

    private static final String TAG = "PutFeedRequest";

    private final String _password;
    private final String _defaultRole;
    private final String _description;
    private final String _chatRoom;
    private final String _tool;
    private final List<String> _groups;

    private String _packagePath;

    public PutFeedRequest(Feed feed, String password) {
        super(feed);
        _password = password;
        _defaultRole = feed.defaultRole.toServerString();
        _description = feed.description;
        _chatRoom = feed.chatRoom;
        _tool = feed.tool;
        _groups = feed.getGroups();
    }

    public PutFeedRequest(Feed feed) {
        this(feed, AuthManager.getPassword(feed));
    }

    public PutFeedRequest(JSONObject json) throws JSONException {
        super(json);
        JSONData data = new JSONData(json, true);
        _description = data.get("Description");
        _chatRoom = data.get("ChatRoom");
        _defaultRole = data.get("DefaultRole");
        _password = data.get("Password");
        _tool = data.get("tool");
        _packagePath = data.get("packagePath");
        _groups = data.getList("Groups", Feed.GROUP_ANON);
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        if (checkSupport(TAKServerVersion.SUPPORT_AUTH)) {
            sb.addParam("password", _password);
            sb.addParam("defaultRole", _defaultRole);
        }
        sb.addParam("creatorUid", getCreatorUID());
        sb.addParam("description", _description);
        sb.addParam("chatRoom", _chatRoom);
        sb.addParam("tool", _tool);
        sb.addParam("packagePath", _packagePath);

        // Publish to anonymous group for now
        StringBuilder groups = new StringBuilder();
        for (int i = 0; i < _groups.size(); i++) {
            String group = _groups.get(i);
            if (FileSystemUtils.isEmpty(group) || FileSystemUtils.isEquals(
                    group, StringUtils.getString(
                            R.string.mission_group_default_label))) {
                group = Feed.GROUP_ANON;
            }
            groups.append(group);
            if (i < _groups.size() - 1)
                groups.append(',');
        }
        sb.addParam("group", groups.toString());
        return sb.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            if (!FileSystemUtils.isEmpty(_password))
                json.put("Password", _password);
            json.put("Description", _description);
            json.put("ChatRoom", _chatRoom);
            json.put("DefaultRole", _defaultRole);
            json.put("tool", _tool);
            json.put("packagePath", _packagePath);

            JSONArray arr = new JSONArray();
            for (String group : _groups)
                arr.put(group);
            json.put("Groups", arr);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON");
        }
        return json;
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_create_feed,
                getFeedName());
    }

    public void setPackagePath(String packagePath) {
        this._packagePath = packagePath;
    }

    public String getPackagePath() {
        return _packagePath;
    }
}
