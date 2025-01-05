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
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * HTTP REST put data package
 */
public class PutDataPackageRequest extends AbstractRequest {

    private static final String TAG = "PutDataPackageRequest";

    private final String _packagePath;
    private final String _packageUID;

    protected PutDataPackageRequest(Feed feed, String packagePath,
                                    String packageUID) {
        super(feed);
        _packagePath = packagePath;
        _packageUID = packageUID;
    }

    public PutDataPackageRequest(Feed feed, MissionPackageManifest manifest) {
        this(feed, manifest.getPath(), manifest.getUID());
    }

    public PutDataPackageRequest(JSONObject json) throws JSONException {
        super(json);
        _packagePath = json.getString("PackagePath");
        _packageUID = json.getString("PackageUID");
    }

    @Override
    public boolean isValid() {
        return super.isValid() && !FileSystemUtils.isEmpty(_packagePath);
    }

    public String getPackagePath() {
        return _packagePath;
    }

    public String getPackageUid() {
        return _packageUID;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("contents/missionpackage");
        sb.addParam("creatorUid", getCreatorUID());
        return sb.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("PackagePath", _packagePath);
            json.put("PackageUID", _packageUID == null ? "" : _packageUID);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize JSON", e);
        }
        return json;
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_DATA_PACKAGE;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(
                R.string.mn_failed_to_publish_data_package,
                getFeedName());
    }
}
