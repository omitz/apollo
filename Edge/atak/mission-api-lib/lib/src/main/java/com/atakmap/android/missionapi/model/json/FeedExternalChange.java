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

import android.graphics.drawable.Drawable;

import com.atakmap.android.missionapi.model.xml.XMLMissionChangeData;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.IconUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;

/**
 * Feed change for external data
 */
public class FeedExternalChange extends FeedChange {

    public FeedExternalDetails details;

    public FeedExternalChange(Feed feed) {
        super(feed);
    }

    public FeedExternalChange(Feed feed, JSONData data) {
        super(feed, data);
        this.details = new FeedExternalDetails(data.getChild("externalData"));
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = super.toJSON(server);
        data.set("externalData", this.details);
        return data;
    }

    @Override
    public String getTitle() {
        String notes = details.getNotes();
        if (!FileSystemUtils.isEmpty(notes))
            return notes;
        return super.getTitle();
    }

    @Override
    public String getContentUID() {
        return this.details.uid;
    }

    @Override
    public String getContentTitle() {
        return this.details.name;
    }

    @Override
    public FeedContent getContent() {
        FeedContent content = super.getContent();
        if (content != null)
            return content;

        // Generate content based on metadata
        return Feed.createExternalData(this);
    }

    @Override
    public Drawable getIconDrawable() {
        return IconUtils.getDrawable(R.drawable.ic_external_data);
    }

    public FeedExternalChange(Feed feed, XMLMissionChangeData xml) {
        super(feed, xml);
        this.details = new FeedExternalDetails(xml.externalData);
    }
}
