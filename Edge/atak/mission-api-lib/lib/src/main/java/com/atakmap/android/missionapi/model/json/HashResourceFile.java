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

package com.atakmap.android.missionapi.model.json;

import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.net.offline.OfflineRequest;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

/**
 * Created by byoung on 10/4/17.
 */
public class HashResourceFile {

    private ResourceFile mFile;
    private String mHash;

    public HashResourceFile(String hash, ResourceFile mFile) {
        this.mHash = hash;
        this.mFile = mFile;
    }

    public ResourceFile getFile() {
        return mFile;
    }

    public void setFile(ResourceFile mFile) {
        this.mFile = mFile;
    }

    public String getHash() {
        return mHash;
    }

    public void setHash(String mHash) {
        this.mHash = mHash;
    }

    public boolean isValid() {
        return !FileSystemUtils.isEmpty(mHash)
                && mFile != null && mFile.isValid();
    }

    public static HashResourceFile fromJSON(JSONObject json)
            throws JSONException {
        HashResourceFile r = new HashResourceFile(
                json.has("Hash") ? json.getString("Hash") : null,
                json.has("ResourceFile")
                        ? ResourceFile
                                .fromJSON(json.getJSONObject("ResourceFile"))
                        : null);

        if (!r.isValid())
            throw new JSONException("Invalid HashResourceFile");

        return r;
    }

    public JSONObject toJSON() throws JSONException {
        if (!isValid())
            throw new JSONException("Invalid HashResourceFile");

        JSONObject json = new JSONObject();
        if (!FileSystemUtils.isEmpty(mHash))
            json.put("Hash", mHash);
        if (mFile != null)
            json.put("ResourceFile", mFile.toJSON());
        return json;
    }

    @Override
    public String toString() {
        if (!isValid())
            return super.toString();

        return mHash + ", " + mFile.toString();
    }

    public OfflineRequest toOfflineRequest(Feed feed) {
        File file = new File(mFile.getFilePath());
        FeedFileChange change = new FeedFileChange(feed);
        change.type = FeedChange.Type.ADD_CONTENT;
        change.setTimestamp(new CoordinatedTime().getMilliseconds());
        change.feedName = feed.name;
        change.creatorUID = MapView.getDeviceUid();

        FeedFileDetails d = new FeedFileDetails();
        d.name = file.getName();
        d.mimeType = mFile.getMimeType();
        d.size = mFile.getSize();
        d.hash = mHash;
        d.contentType = mFile.getContentType();
        d.uid = UUID.nameUUIDFromBytes(mHash.getBytes()).toString();
        change.details = d;
        return new OfflineRequest(feed, change, file.getAbsolutePath());
    }
}
