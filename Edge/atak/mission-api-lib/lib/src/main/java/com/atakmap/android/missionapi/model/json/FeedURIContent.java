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

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.atakmap.android.data.URIContentHandler;
import com.atakmap.android.hashtags.HashtagContent;
import com.atakmap.android.hashtags.util.HashtagSet;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.hierarchy.action.Search;
import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.hierarchy.items.MapItemUser;
import com.atakmap.android.maps.ILocation;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.missionapi.model.xml.XMLMissionContent;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.MutableGeoBounds;

/**
 * Feed content which is represented by a URI (i.e. files, external data)
 */
public abstract class FeedURIContent extends FeedContent implements
        GoTo, MapItemUser, Visibility, ILocation {

    protected FeedURIContent(Feed feed) {
        super(feed);
    }

    protected FeedURIContent(Feed feed, JSONData data) {
        super(feed, data);
    }

    /**
     * Get a URI content handler for this item
     * Return null if N/A
     */
    public abstract URIContentHandler getContentHandler();

    /**
     * Delete local content for this item via its content handler
     */
    @Override
    public boolean removeLocalContent() {
        URIContentHandler handler = getContentHandler();
        if (handler != null) {
            handler.deleteContent();
            return true;
        }
        return false;
    }

    @Override
    public String getTitle() {
        URIContentHandler handler = getContentHandler();
        return handler != null ? handler.getTitle() : null;
    }

    @Override
    public Drawable getIconDrawable() {
        URIContentHandler handler = getContentHandler();
        return handler != null ? handler.getIcon() : null;
    }

    @Override
    public int getIconColor() {
        URIContentHandler handler = getContentHandler();
        return handler != null ? handler.getIconColor() : Color.WHITE;
    }

    @Override
    public void setLocalHashtags(HashtagSet tags) {
        URIContentHandler handler = getContentHandler();
        if (handler instanceof HashtagContent)
            ((HashtagContent) handler).setHashtags(tags);
    }

    @Override
    public boolean search(String terms) {
        URIContentHandler handler = getContentHandler();
        if (handler != null) {
            if (StringUtils.find(handler.getTitle(), terms))
                return true;
            if (handler instanceof Search && !((Search) handler)
                    .find(terms).isEmpty())
                return true;
        }
        return super.search(terms);
    }

    @Override
    public boolean goTo(boolean select) {
        URIContentHandler handler = getContentHandler();
        return handler instanceof GoTo && ((GoTo) handler).goTo(select);
    }

    @Override
    public MapItem getMapItem() {
        URIContentHandler handler = getContentHandler();
        return handler instanceof MapItemUser
                ? ((MapItemUser) handler).getMapItem() : null;
    }

    @Override
    public boolean setVisible(boolean visible) {
        URIContentHandler handler = getContentHandler();
        return handler instanceof Visibility
                && ((Visibility) handler).setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        URIContentHandler handler = getContentHandler();
        return handler instanceof Visibility
                && ((Visibility) handler).isVisible();
    }

    @Override
    public GeoPoint getPoint(GeoPoint point) {
        URIContentHandler handler = getContentHandler();
        if (handler instanceof ILocation)
            return ((ILocation) handler).getPoint(point);
        return point;
    }

    @Override
    public GeoBounds getBounds(MutableGeoBounds bounds) {
        URIContentHandler handler = getContentHandler();
        if (handler instanceof ILocation)
            return ((ILocation) handler).getBounds(bounds);
        return bounds;
    }

    public FeedURIContent(Feed feed, XMLMissionContent mf) {
        super(feed, mf);
    }
}
