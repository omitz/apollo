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

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.atakmap.android.data.URIScheme;
import com.atakmap.android.maps.MapTouchController;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.model.xml.XMLContentUidData;
import com.atakmap.android.missionapi.model.xml.XMLContentUidDetails;
import com.atakmap.android.missionapi.model.xml.XMLCreatorData;
import com.atakmap.android.missionapi.model.xml.XMLMissionCoT;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.android.missionapi.util.IconUtils;
import com.atakmap.android.hashtags.util.HashtagSet;
import com.atakmap.android.hierarchy.items.MapItemUser;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.AnchoredMapItem;
import com.atakmap.android.maps.CrumbTrail;
import com.atakmap.android.maps.ILocation;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.android.missionpackage.file.MissionPackageManifest;
import com.atakmap.android.routes.Route;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.android.video.VideoDropDownReceiver;
import com.atakmap.android.video.manager.VideoManager;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.MutableGeoBounds;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Map item or video alias within a feed
 */
public class FeedCoT extends FeedContent implements MapItemUser, ILocation {

    protected static final String TAG = "FeedCoT";

    public String uid;
    public Set<String> contentHashes = new HashSet<>();
    public FeedCoTDetails details;

    protected FeedCoT(Feed feed) {
        super(feed);
    }

    protected FeedCoT(Feed feed, JSONData data) {
        super(feed, data);
        this.uid = data.get("data");
        this.contentHashes.addAll(data.getList("contentHashes", ""));
        this.details = new FeedCoTDetails(data.getChild("details"));
    }

    protected FeedCoT(FeedCoTChange change) {
        super(change);
        update(change);
    }

    protected FeedCoT(Feed feed, MapItem item) {
        super(feed);
        this.uid = item.getUID();
        this.details = new FeedCoTDetails();
        this.details.title = item.getTitle();
        this.details.type = item.getType();
        this.details.color = item.getIconColor();
        this.details.location = getPoint(null);
    }

    protected FeedCoT(Feed feed, ConnectionEntry ce) {
        super(feed);
        this.uid = ce.getUID();
        this.details = new FeedCoTDetails();
        this.details.title = ce.getAlias();
        this.details.type = "b-i-v";
    }

    public void update(FeedCoTChange change) {
        super.update(change);
        this.uid = change.contentUID;
        this.details = new FeedCoTDetails(change.details);
    }

    @Override
    public void update(FeedEntry entry, boolean fromServer) {
        super.update(entry, fromServer);
        if (entry instanceof FeedCoT) {
            FeedCoT cot = (FeedCoT) entry;
            this.uid = cot.getUID();
            this.contentHashes.clear();
            this.contentHashes.addAll(cot.contentHashes);
            this.details = new FeedCoTDetails(cot.details);
        }
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = super.toJSON(server);
        data.set("data", this.uid);
        data.set("details", this.details);
        data.set("contentHashes", this.contentHashes);
        return data;
    }

    @Override
    public String getTitle() {
        String title;
        ConnectionEntry cEntry = getVideoAlias();
        if (cEntry != null) {
            title = cEntry.getAlias();
            if (!FileSystemUtils.isEmpty(title))
                return title;
        }

        MapItem mapItem = getMapItem();
        if (mapItem != null) {
            title = mapItem.getTitle();
            if (!FileSystemUtils.isEmpty(title))
                return title;
        }

        return !FileSystemUtils.isEmpty(details.title) ? details.title
                : StringUtils.getString(R.string.content_untitled);
    }

    @Override
    public String getUID() {
        return this.uid;
    }

    @Override
    public String getURI() {
        return URIScheme.MAP_ITEM + getUID();
    }

    @Override
    public String getGroupType() {
        String type = getType();
        if ("b-i-v".equals(type))
            return "file/video-alias";
        else if (type != null)
            return "cot/" + type;
        return "cot/unknown";
    }

    /**
     * Get the CoT event type
     * @return Type
     */
    public String getType() {
        ConnectionEntry cEntry = getVideoAlias();
        if (cEntry != null)
            return "b-i-v";

        MapItem mapItem = getMapItem();
        if (mapItem != null)
            return mapItem.getType();

        return this.details.type;
    }

    @Override
    public Drawable getIconDrawable() {
        ConnectionEntry cEntry = getVideoAlias();
        if (cEntry != null)
            return IconUtils.getDrawable(R.drawable.ic_video_alias);

        Drawable icon = null;
        MapItem mapItem = getMapItem();
        if (mapItem != null)
            icon = mapItem.getIconDrawable();
        if (icon == null)
            icon = IconUtils.getIconDrawable(this.details.iconURI);

        return icon != null ? icon : IconUtils.getDrawable(
                R.drawable.ic_unknown);
    }

    @Override
    public int getIconColor() {
        MapItem mapItem = getMapItem();
        if (mapItem != null)
            return ATAKUtilities.getIconColor(mapItem);

        return this.details.color;
    }

    @Override
    public boolean isStoredLocally() {
        MapItem mapItem = getMapItem();
        ConnectionEntry cEntry = getVideoAlias();
        return mapItem != null && mapItem.getGroup() != null || cEntry != null;
    }

    @Override
    public boolean removeLocalContent() {
        MapItem item = getMapItem();
        ConnectionEntry cEntry = getVideoAlias();
        if (item != null) {
            item.setMetaBoolean("dsLocallyRemoved", true);
            if (item.getMetaBoolean("removable", true)) {
                // Remove from map group
                item.removeFromGroup();
            } else if (item.hasMetaValue("deleteAction")) {
                // Special delete action
                Intent delete = new Intent(item
                        .getMetaString("deleteAction", ""));
                delete.putExtra("targetUID", item.getUID());
                AtakBroadcast.getInstance().sendBroadcast(delete);
            }
        } else if (cEntry != null) {
            cEntry.setTemporary(true);
            VideoManager.getInstance().removeEntry(cEntry);
        }
        return true;
    }

    public void setContentHashes(Collection<String> hashes) {
        this.contentHashes.clear();
        this.contentHashes.addAll(hashes);
    }

    @Override
    public Collection<String> getHashtags() {
        MapItem mi = getMapItem();
        if (mi != null) {
            // Include map item-specific hashtags
            HashtagSet tags = new HashtagSet(super.getHashtags());
            tags.addAll(mi.getHashtags());
            return tags;
        }
        return super.getHashtags();
    }

    @Override
    public HashtagSet getLocalHashtags() {
        MapItem mi = getMapItem();
        if (mi != null)
            return new HashtagSet(mi.getHashtags());
        return super.getLocalHashtags();
    }

    @Override
    public void setLocalHashtags(HashtagSet tags) {
        MapItem mi = getMapItem();
        if (mi != null) {
            mi.toggleMetaData("dsLocallyModified", true);
            mi.setHashtags(tags);
        }
    }

    @Override
    public boolean download() {
        return false;
    }

    @Override
    public boolean search(String terms) {
        MapItem mapItem = getMapItem();
        ConnectionEntry cEntry = getVideoAlias();
        if (mapItem != null) {
            if (StringUtils.find(mapItem.getTitle(), terms))
                return true;
        } else if (cEntry != null) {
            if (StringUtils.find(cEntry.getAlias(), terms)
                    || StringUtils.find(cEntry.getAddress(), terms)
                    || StringUtils.find(cEntry.getPath(), terms))
                return true;
        }
        return super.search(terms);
    }

    @Override
    public MapItem getMapItem() {
        return FeedUtils.findMapItem(this.uid);
    }

    public Shape getShape() {
        MapItem mapItem = getMapItem();
        if (mapItem != null && mapItem.getGroup() != null
                && mapItem.hasMetaValue("shapeUID")) {
            MapItem shape = FeedUtils.findMapItem(mapItem
                    .getMetaString("shapeUID", null));
            if (shape instanceof Shape)
                return (Shape) shape;
        }
        return null;
    }

    public ConnectionEntry getVideoAlias() {
        return VideoManager.getInstance().getEntry(this.uid);
    }

    public boolean hasVideoAlias() {
        return getVideoAlias() != null;
    }

    public Set<String> getAttachmentHashes() {
        return Collections.emptySet();
    }

    /**
     * Check if this CoT has an attachment with a given hash
     * @param hash Hash string
     * @return True if contained
     */
    public boolean hasAttachmentHash(String hash) {
        return false;
    }

    @Override
    public long getEstimatedFileSize() {
            return MissionPackageManifest.MAP_ITEM_ESTIMATED_SIZE;
    }

    @Override
    public void cache() {
    }

    @Override
    public boolean goTo(boolean select) {
        MapView mv = MapView.getMapView();
        if (mv == null)
            return false;
        MapItem item = getMapItem();
        ConnectionEntry cEntry = getVideoAlias();
        if (item != null) {
            Shape shp = getShape();
            if (shp != null)
                item = shp;
            if (!item.getVisible()) {
                item.setVisible(true);
                item.persist(mv.getMapEventDispatcher(), null, getClass());
            }
            return MapTouchController.goTo(item, select);
        } else if (cEntry != null) {
            AtakBroadcast.getInstance().sendBroadcast(new Intent(
                    VideoDropDownReceiver.DISPLAY)
                    .putExtra("CONNECTION_ENTRY", cEntry));
        } else if (details.location != null)
            ATAKUtilities.scaleToFit(this);
        return false;
    }

    @Override
    public GeoPoint getPoint(GeoPoint point) {
        GeoPoint ret = details.location;
        MapItem mi = getMapItem();
        if (mi instanceof Route && ((Route) mi).getNumPoints() > 0)
            ret = ((Route) mi).getPoint(0).get();
        else if (mi instanceof Shape)
            ret = ((Shape) mi).getCenter().get();
        else if (mi instanceof PointMapItem)
            ret = ((PointMapItem) mi).getPoint();
        else if (mi instanceof AnchoredMapItem)
            ret = ((AnchoredMapItem) mi).getAnchorItem().getPoint();
        else if (mi instanceof CrumbTrail)
            ret = ((CrumbTrail) mi).getTarget().getPoint();
        if (ret != null && point != null && point.isMutable()) {
            point.set(ret);
            return point;
        }
        return ret;
    }

    @Override
    public GeoBounds getBounds(MutableGeoBounds bounds) {
        MapItem mi = getMapItem();
        if (mi instanceof Shape)
            return ((Shape) mi).getBounds(bounds);
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FeedCoT feedCoT = (FeedCoT) o;
        return Objects.equals(uid, feedCoT.uid) &&
                Objects.equals(contentHashes, feedCoT.contentHashes) &&
                Objects.equals(details, feedCoT.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uid, contentHashes, details);
    }

    protected FeedCoT(Feed feed, XMLMissionCoT mc) {
        super(feed, mc);
        XMLContentUidData data = mc._metadata;
        if (data != null) {
            this.uid = data.uid;
            XMLContentUidDetails details = data.details;
            if (details != null) {
                this.details = new FeedCoTDetails();
                this.details.title = details.callsign;
                if (this.details.title == null)
                    this.details.title = details.title;
                this.details.color = details.color != null
                        ? details.color : Color.WHITE;
                this.details.type = details.type;
                this.details.iconsetPath = details.iconsetPath;
                this.details.iconURI = IconUtils.getIconURI(
                        this.details.type, this.details.iconsetPath);
            }
            XMLCreatorData creatorData = data.creatorData;
            if (creatorData != null) {
                this.creatorUID = creatorData.creatorUid;
                setTimestamp(creatorData.timestamp);
            }
            String[] kw = data.keywords;
            if (!FileSystemUtils.isEmpty(kw))
                setHashtags(Arrays.asList(kw));
        }
    }
}
