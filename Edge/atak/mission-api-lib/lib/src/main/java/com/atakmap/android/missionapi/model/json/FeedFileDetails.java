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

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.atakmap.android.missionapi.model.xml.XMLContentResourceData;
import com.atakmap.android.missionapi.util.IconUtils;
import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.filesystem.ResourceFile.MIMEType;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * File resource
 */
public class FeedFileDetails implements JSONSerializable {

    public static final String CONTENT_TYPE = "CONTENTTYPE_";

    public String name, mimeType, contentType, uid, hash;
    public String creatorUID, timestamp;
    public long size;

    public FeedFileDetails() {
    }

    public FeedFileDetails(JSONData data) {
        this.name = data.get("name");
        this.uid = data.get("uid");
        this.hash = data.get("hash");
        this.size = data.get("size", 0L);
        this.mimeType = data.get("mimeType");

        String cType = data.get("contentType");

        // XXX - Content type is stored in keywords because reasons
        List<String> contentTypes = data.getList("keywords", "");
        if (FileSystemUtils.isEmpty(cType) && !contentTypes.isEmpty()) {
            cType =  contentTypes.get(0);
            if (cType.startsWith(CONTENT_TYPE))
                cType = cType.substring(CONTENT_TYPE.length());
        }

        this.contentType = cType;

        JSONData creatorData = data.getChild("creatorData");
        if (creatorData != null) {
            this.creatorUID = creatorData.get("creatorUid");
            this.timestamp = creatorData.get("timestamp");
        }
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = new JSONData(server);
        data.set("name", this.name);
        data.set("uid", this.uid);
        data.set("hash", this.hash);
        data.set("size", this.size);
        data.set("mimeType", this.mimeType);

        if (server) {
            // Need to convert content type back to ugly keywords array to
            // appease the server
            List<String> keywords = new ArrayList<>();
            if (!FileSystemUtils.isEmpty(this.contentType))
                keywords.add(CONTENT_TYPE + this.contentType);
            data.set("keywords", keywords);
        } else
            data.set("contentType", this.contentType);

        JSONData creatorData = new JSONData(server);
        creatorData.set("creatorUid", this.creatorUID);
        creatorData.set("timestamp", this.timestamp);
        if (creatorData.length() > 0)
            data.set("creatorData", creatorData);

        return data;
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSize() {
        return size;
    }

    public Drawable getIconDrawable() {
        Drawable dr = IconUtils.getContentIcon(name, contentType);
        if (dr != null)
            return dr;

        // Low-res file extension based icon bitmaps
        MIMEType mt = ResourceFile.getMIMETypeForMIME(this.mimeType);
        if (mt == null)
            mt = ResourceFile.getMIMETypeForFile(this.name);
        if (mt == null || mt.ICON_URI == null)
            return null;
        Bitmap icon = ATAKUtilities.getUriBitmap(mt.ICON_URI);
        return icon != null ? new BitmapDrawable(icon) : null;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public FeedFileDetails(XMLContentResourceData xml) {
        this.name = xml.name;
        this.creatorUID = xml.creatorUid;
        this.name = xml.name;
        this.uid = xml.uid;
        this.hash = xml.hash;
        this.size = xml.size;
        this.mimeType = xml.mimeType;
        this.contentType = xml.contentType;
        if (FileSystemUtils.isEmpty(this.contentType)
                && !FileSystemUtils.isEmpty(xml.keywordString)) {
            this.contentType = xml.keywordString;
            if (this.contentType.startsWith(CONTENT_TYPE))
                this.contentType = this.contentType.substring(
                        CONTENT_TYPE.length());
        }
    }
}
