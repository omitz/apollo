package com.atakmap.android.missionapi.data;

import com.atakmap.android.data.URIContentHandler;
import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.image.ImageDropDownReceiver;
import com.atakmap.android.maps.AnchoredMapItem;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.missionapi.interfaces.FileSelection;
import com.atakmap.android.missionapi.model.json.AttachmentList;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.put.PutFeedFilesRequest;
import com.atakmap.android.missionapi.net.http.put.PutFeedItemsRequest;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.android.routes.Route;
import com.atakmap.android.toolbars.RangeAndBearingMapItem;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AttachmentManager;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.filesystem.HashingUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles publish interactions for a specific feed
 */
public class FeedPublishManager implements FileSelection {

    private static final String TAG = "FeedPublishManager";

    protected final Feed _feed;

    public FeedPublishManager(Feed feed) {
        _feed = feed;
    }

    /**
     * Publish a list of files to the mission
     * @param files Files
     */
    public void publishFiles(List<File> files) {
        if (FileSystemUtils.isEmpty(files))
            return;
        List<ResourceFile> fileList = new ArrayList<>(files.size());
        for (File f : files)
            fileList.add(new ResourceFile(f.getAbsolutePath(), null));
        RestManager.getInstance().executeRequest(new PutFeedFilesRequest(
                _feed, fileList));
    }

    public void publishFile(File f) {
        if (f == null || !f.exists())
            return;
        publishFiles(Collections.singletonList(f));
    }

    public void publishItem(MapItem item, boolean ignoreAttachments) {
        publishItemImpl(item, ignoreAttachments, null);
    }

    public void publishItem(MapItem item) {
        publishItem(item, false);
    }

    public void publishItems(List<MapItem> items, boolean ignoreAttachments) {
        if (FileSystemUtils.isEmpty(items))
            return;

        // Make sure we're only publishing one instance of the item per request
        Set<String> published = new HashSet<>();
        for (MapItem item : items)
            publishItemImpl(item, ignoreAttachments, published);
    }

    public void publishItems(List<MapItem> items) {
        publishItems(items, false);
    }

    /**
     * Publish a map item and any associated items
     * @param item Map item
     * @param ignoreAtt True to ignore attachments
     * @param published Set of already published UIDs
     */
    protected void publishItemImpl(MapItem item, boolean ignoreAtt,
                                 Set<String> published) {
        if (published == null)
            published = new HashSet<>();
        List<MapItem> associated = findAssociatedItems(item);
        for (MapItem mi : associated) {
            if (FeedUtils.canPublish(mi) && !published.contains(mi.getUID())) {
                publishSingleItemImpl(item, ignoreAtt);
                published.add(mi.getUID());
            }
        }
    }

    protected void publishSingleItemImpl(MapItem mi, boolean ignoreAtt) {
        List<AttachmentList> attachments = new ArrayList<>();
        if (!ignoreAtt) {
            String uid = mi.getUID();
            List<String> localPaths = new ArrayList<>();
            List<String> hashes = new ArrayList<>();
            List<File> atts = AttachmentManager.getAttachments(uid);
            for (File f : atts) {
                localPaths.add(f.getAbsolutePath());
                hashes.add(HashingUtils.sha256sum(f));
            }
            attachments.add(new AttachmentList(uid, null, localPaths,
                    hashes, null));
        }
        RestManager.getInstance().executeRequest(
                new PutFeedItemsRequest(_feed, true, attachments));
    }

    /**
     * Find any map items associated with a given parent item which should be
     * published together (including the parent item itself)
     * @param item Map item
     * @return List of items
     */
    protected List<MapItem> findAssociatedItems(MapItem item) {
        List<MapItem> ret = new ArrayList<>();
        if (item == null)
            return ret;

        // R&B line end points
        if (item instanceof RangeAndBearingMapItem) {
            RangeAndBearingMapItem line = (RangeAndBearingMapItem) item;
            ret.add(line.getPoint1Item());
            ret.add(line.getPoint2Item());
        }

        // Shape anchors
        else if (item instanceof AnchoredMapItem) {
            AnchoredMapItem mi = (AnchoredMapItem) item;
            ret.add(mi.getAnchorItem());
        }

        // Route waypoint -> Route
        else if (item.getType().equals(Route.WAYPOINT_TYPE)) {
            String routeUID = item.getMetaString("parent_route_uid", null);
            ret.add(FeedUtils.findMapItem(routeUID));
            return ret;
        }

        // Check for generic shape association
        else if (!FeedUtils.canPublish(item)) {
            MapItem shp = ATAKUtilities.findAssocShape(item);
            if (shp != item)
                ret.add(shp);
            return ret;
        }

        ret.add(item);
        return ret;
    }

    /**
     * Check if a file is already loaded/imported
     * @param file File file
     * @return True if is loaded
     */
    protected boolean isLoaded(File file) {
        if (!FileSystemUtils.isFile(file))
            return false;

        URIContentHandler handler = URIContentManager.getInstance()
                .getHandler(file);
        if (handler != null)
            return true;

        if (ImageDropDownReceiver.ImageFileFilter.accept(file.getParentFile(),
                file.getName()))
            return true;

        return false;
    }

    @Override
    public boolean onFilesSelected(List<File> files) {
        publishFiles(files);
        return true;
    }
}
