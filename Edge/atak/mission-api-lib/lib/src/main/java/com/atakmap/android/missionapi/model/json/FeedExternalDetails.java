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

import android.util.Base64;

import com.atakmap.android.data.URIHelper;
import com.atakmap.android.missionapi.model.xml.XMLExternalDetails;
import com.atakmap.coremap.filesystem.FileSystemUtils;

/**
 * External data details
 */
public class FeedExternalDetails implements JSONSerializable {

    // For use with content handlers
    public static final String URI_SCHEME = "feed-external-data://";

    public String uid, name, tool, urlData, urlView, notes;

    public FeedExternalDetails() {
    }

    public FeedExternalDetails(JSONData data) {
        this.uid = data.get("uid");
        this.name = data.get("name");
        this.tool = data.get("tool");
        this.urlData = data.get("urlData");
        this.urlView = data.get("urlView");
        this.notes = data.get("notes");
    }

    public FeedExternalDetails(String contentURI) throws IllegalArgumentException {
        this(new JSONData(new String(Base64.decode(
                URIHelper.getContent(URI_SCHEME, contentURI),
                Base64.DEFAULT), FileSystemUtils.UTF8_CHARSET), true));
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = new JSONData(server);
        data.set("uid", this.uid);
        data.set("name", this.name);
        data.set("tool", this.tool);
        data.set("urlData", this.urlData);
        data.set("urlView", this.urlView);
        data.set("notes", this.notes);
        return data;
    }

    public String getUID() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getTool() {
        return tool;
    }

    public String getDataURL() {
        return urlData;
    }

    public String getViewURL() {
        return urlView;
    }

    public String getNotes() {
        return notes;
    }

    /**
     * Convert metadata for this link to a base64 content URI string
     * For use with URI-based content handlers
     * @return Content URI
     */
    public String toContentURI() {
        JSONData data = toJSON(true);
        return URI_SCHEME + Base64.encodeToString(
                data.toString().getBytes(FileSystemUtils.UTF8_CHARSET),
                Base64.DEFAULT);
    }

    public FeedExternalDetails(XMLExternalDetails xml) {
        this.uid = xml.uid;
        this.name = xml.name;
        this.notes = xml.notes;
        this.tool = xml.tool;
        this.urlData = xml.urlData;
        this.urlView = xml.urlView;
    }
}
