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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atakmap.android.missionapi.model.json.AttachmentList;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.util.List;

/**
 * HTTP REST request to add map items to a feed
 */
public class PutFeedItemsRequest extends AbstractRequest {

    private static final String TAG = "PutFeedItemsRequest";

    private List<AttachmentList> _attachments;

    /**
     * True to publish the feed Item to the feed. False to skip, and only publish attachments
     */
    private boolean _putItem;

    public PutFeedItemsRequest(Feed feed, boolean bPutItem,
                               List<AttachmentList> attachments) {
        super(feed);
        _attachments = attachments;
        _putItem = bPutItem;
    }
    
    public PutFeedItemsRequest(JSONObject json) throws JSONException {
        super(json);
        _putItem = json.getBoolean("PutItem");
        if (json.has("Attachments"))
            _attachments = AttachmentList.fromJSONList(new JSONObject(
                    json.getString("Attachments")));
    }

    public List<AttachmentList> getAttachments() {
        return _attachments;
    }

    public void setAttachments(List<AttachmentList> attachments) {
        _attachments = attachments;
    }

    public boolean hasAttachments() {
        return !FileSystemUtils.isEmpty(_attachments);
    }

    public boolean isPutItem() {
        return _putItem;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("PutItem", _putItem);
            if (hasAttachments()) {
                json.put("Attachments", AttachmentList.toJSONList(_attachments)
                        .toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }

        return json;
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED_ITEMS;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_publish_items,
                getFeedName());
    }

    /**
     * Get request body in JSON format
     * @param uid
     * @return
     * @throws JSONException
     */
    public static String getRequestBody(String uid) throws JSONException {
        if (FileSystemUtils.isEmpty(uid)) {
            throw new JSONException("No uid to publish");
        }

        JSONObject json = new JSONObject();
        JSONArray hashArray = new JSONArray();
        json.put("uids", hashArray);
        hashArray.put(uid);

        return json.toString();
    }
}
