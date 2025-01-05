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

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedContent;
import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.hashtags.util.HashtagSet;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * HTTP REST put feed hashtags
 */
public class PutFeedKeywordsRequest extends AbstractRequest {

    private static final String TAG = "PutFeedKeywordsRequest";

    private final HashtagSet _hashtags;
    private String _hash, _uid;

    public PutFeedKeywordsRequest(Feed feed, HashtagSet hashtags) {
        super(feed);
        _hashtags = hashtags;
    }

    public PutFeedKeywordsRequest(Feed feed, FeedContent content,
                                  HashtagSet hashtags) {
        this(feed, hashtags);
        if (content instanceof FeedFile)
            _hash = ((FeedFile) content).getHash();
        else
            _uid = content.getUID();
    }
    public PutFeedKeywordsRequest(JSONObject json) throws JSONException {
        super(json);
        _hashtags = new HashtagSet();
        if (json.has("Keywords")) {
            JSONArray arr = json.getJSONArray("Keywords");
            for (int i = 0; i < arr.length(); i++)
                _hashtags.add(arr.getString(i));
        }
        _uid = json.has("UID") ? json.getString("UID") : null;
        _hash = json.has("Hash") ? json.getString("Hash") : null;
    }

    public HashtagSet getHashtags() {
        return _hashtags;
    }

    @Override
    public String getContentUID() {
        return _uid != null ? _uid : _hash;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        if (_hash != null)
            sb.addPath("content").addPath(_hash);
        else if (_uid != null)
            sb.addPath("uid").addPath(_uid);
        sb.addPath("keywords");
        return sb.toString();
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED_KEYWORDS;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_publish_hashtags,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            if (!_hashtags.isEmpty()) {
                JSONArray arr = new JSONArray();
                for (String kw : _hashtags)
                    arr.put(kw);
                json.put("Keywords", arr);
            }
            if (_hash != null)
                json.put("Hash", _hash);
            if (_uid != null)
                json.put("UID", _uid);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }
}
