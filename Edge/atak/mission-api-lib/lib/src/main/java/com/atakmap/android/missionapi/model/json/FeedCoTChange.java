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
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;

/**
 * Feed change for CoT content
 */
public class FeedCoTChange extends FeedChange {

    public FeedCoTDetails details;
    public String contentUID;

    public FeedCoTChange(Feed feed) {
        super(feed);
    }

    public FeedCoTChange(Feed feed, JSONData data) {
        super(feed, data);
        this.contentUID = data.get("contentUid");
        this.details = new FeedCoTDetails(data.getChild("details"));
    }

    public FeedCoTChange(Feed feed, FeedCoT cot) {
        super(feed, cot);
        this.contentUID = cot.getUID();
        this.details = new FeedCoTDetails(cot.details);
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = super.toJSON(server);
        data.set("contentUid", this.contentUID);
        data.set("details", this.details);
        return data;
    }

    @Override
    public String getContentUID() {
        return this.contentUID;
    }

    @Override
    public String getContentTitle() {
        return !FileSystemUtils.isEmpty(this.details.title) ? this.details.title
                : StringUtils.getString(R.string.content_untitled);
    }

    @Override
    public FeedContent getContent() {
        FeedContent content = super.getContent();
        if (content != null)
            return content;

        // Generate content based on metadata
        return Feed.createCoT(this);
    }

    @Override
    public Drawable getIconDrawable() {
        Drawable icon = this.details.getIconDrawable();
        return icon != null ? icon : IconUtils.getDrawable(
                R.drawable.ic_unknown);
    }

    @Override
    public int getIconColor() {
        return this.details.color;
    }

    public FeedCoTChange(Feed feed, XMLMissionChangeData change) {
        super(feed, change);
        this.contentUID = change.contentUid;
        this.details = new FeedCoTDetails(change.details);
    }
}
