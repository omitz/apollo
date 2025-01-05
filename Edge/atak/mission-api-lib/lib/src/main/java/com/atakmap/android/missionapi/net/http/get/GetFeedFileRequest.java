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

import org.json.JSONException;
import org.json.JSONObject;

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.android.missionapi.model.json.FeedFileDetails;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.RetryRequest;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;

/**
 * Created by byoung on 4/12/2016.
 */
public class GetFeedFileRequest extends RetryRequest {

    private static final String TAG = "GetFeedFileRequest";

    private final FeedFileDetails _details;
    private final boolean _silent;

    private File _destDir;

    public GetFeedFileRequest(Feed feed, FeedFile file, boolean silent) {
        super(feed, 0);
        _details = file.getDetails();
        _silent = silent;
    }

    public GetFeedFileRequest(JSONObject json) throws JSONException {
        super(json);
        JSONData details = new JSONData(json.get("FileDetails"), true);
        _details = new FeedFileDetails(details);
        _silent = false;
        if (json.has("Directory"))
            _destDir = new File(json.getString("Directory"));
    }

    /**
     * Set the directory to save the file
     * @param dir Directory
     */
    public void setDestinationDirectory(File dir) {
        _destDir = dir;
    }

    public File getDestinationDirectory() {
        return _destDir;
    }

    public FeedFileDetails getDetails() {
        return _details;
    }

    @Override
    public int getNotificationID() {
        return _silent ? 0 : super.getNotificationID();
    }

    @Override
    public String getContentUID() {
        return _details.hash;
    }

    @Override
    public int getRequestType() {
        return RestManager.GET_FEED_FILE;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder("sync/content");
        sb.addParam("hash", _details.hash);
        return sb.toString();
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_get_files,
                _details.name, getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("FileDetails", _details.toJSON(true).toJSON());
            if (_destDir != null)
                json.put("Directory", _destDir.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
