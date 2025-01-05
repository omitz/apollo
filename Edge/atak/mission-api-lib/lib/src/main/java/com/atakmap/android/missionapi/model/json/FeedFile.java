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
import android.graphics.drawable.Drawable;

import com.atakmap.android.coordoverlay.CoordOverlayMapReceiver;
import com.atakmap.android.data.URIContentHandler;
import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.data.URIHelper;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.data.FileChangeWatcher;
import com.atakmap.android.missionapi.model.xml.XMLContentResourceData;
import com.atakmap.android.missionapi.model.xml.XMLCreatorData;
import com.atakmap.android.missionapi.model.xml.XMLMissionFile;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.filesystem.MIMETypeMapper;
import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.grg.GRGMapComponent;
import com.atakmap.android.hashtags.util.HashtagSet;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.image.ImageDropDownReceiver;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importfiles.sort.ImportResolver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.android.missionapi.util.FileUtils;
import com.atakmap.android.missionapi.util.IconUtils;
import com.atakmap.android.update.AppMgmtUtils;
import com.atakmap.android.user.FocusBroadcastReceiver;
import com.atakmap.android.video.ConnectionEntry;
import com.atakmap.android.video.VideoDropDownReceiver;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

/**
 * File within a feed
 */
public class FeedFile extends FeedURIContent {

    protected static final String TAG = "FeedFile";

    public String name, uid, mimeType, contentType, hash, localPath,
            attachmentUID;
    public long size;

    protected FeedFile(Feed feed) {
        super(feed);
    }

    protected FeedFile(Feed feed, JSONData data) {
        super(feed, data);

        this.localPath = data.get("localPath");
        this.attachmentUID = data.get("attachmentUid");

        setDetails(new FeedFileDetails(data.getChild("data")));
    }

    protected FeedFile(Feed feed, FeedFileDetails details) {
        super(feed);
        setDetails(details);
        this.creatorUID = details.creatorUID;
        setTimestamp(details.timestamp);
    }

    protected FeedFile(FeedFileChange change) {
        super(change.feed);
        update(change);
    }

    public void update(FeedFileChange change) {
        super.update(change);
        setDetails(change.details);
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = super.toJSON(server);
        data.set("localPath", this.localPath);
        data.set("attachmentUid", this.attachmentUID);
        data.set("data", getDetails());
        return data;
    }

    @Override
    public void update(FeedEntry entry, boolean fromServer) {
        super.update(entry, fromServer);
        if (entry instanceof FeedFile) {
            FeedFile file = (FeedFile) entry;
            setDetails(file.getDetails());
            if (!fromServer) {
                this.localPath = file.getLocalPath();
                this.attachmentUID = file.getAttachmentUID();
            }
        }
    }

    /**
     * Set file details
     * @param details Details
     */
    public void setDetails(FeedFileDetails details) {
        this.name = details.name;
        this.uid = details.uid;
        this.hash = details.hash;
        this.size = details.size;
        this.mimeType = details.mimeType;
        this.contentType = details.contentType;
    }

    /**
     * Get file details
     * @return Details
     */
    public FeedFileDetails getDetails() {
        FeedFileDetails details = new FeedFileDetails();
        details.name = this.name;
        details.uid = this.uid;
        details.hash = this.hash;
        details.size = this.size;
        details.mimeType = this.mimeType;
        details.contentType = this.contentType;
        return details;
    }

    @Override
    public String getTitle() {
        String title = super.getTitle();
        return FileSystemUtils.isEmpty(title) ? this.name : title;
    }

    @Override
    public String getUID() {
        return this.uid;
    }

    @Override
    public String getURI() {
        File f = getLocalFile();
        if (f != null)
            return URIHelper.getURI(f);
        return "hash://" + getHash();
    }

    @Override
    public String getGroupType() {
        if (isGRG())
            return "file/grg";
        String ext = FileUtils.getExtension(getName());
        if (!FileSystemUtils.isEmpty(ext)) {
            if (isImage())
                return "image/" + ext;
            return "file/" + ext;
        }
        return "file/unknown";
    }

    public String getName() {
        return this.name != null ? this.name : "<Unknown>";
    }

    public String getHash() {
        return this.hash;
    }

    public String getAttachmentUID() {
        return this.attachmentUID;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.name != null;
    }

    @Override
    public Drawable getIconDrawable() {
        if (isVideo())
            return IconUtils.getDrawable(R.drawable.ic_video_alias);

        Drawable icon = super.getIconDrawable();
        return icon != null ? icon : getDetails().getIconDrawable();
    }

    @Override
    public URIContentHandler getContentHandler() {
        return URIContentManager.getInstance().getHandler(getLocalFile());
    }

    @Override
    public boolean isStoredLocally() {
        return FileSystemUtils.isFile(getLocalFile());
    }

    public String getLocalPath() {
        return this.localPath;
    }

    public File getLocalFile() {
        return this.localPath != null ? new File(this.localPath) : null;
    }

    public void setLocalFile(File f) {
        setLocalPath(f.getAbsolutePath());
    }

    public void setLocalPath(String path) {
        if (this.feed != null)
            this.feed.updateFilePath(this, path);
        this.localPath = path;
    }

    public void setAttachmentUID(String uid) {
        this.attachmentUID = uid;
    }

    /**
     * Find and set the attachment UID if there is one
     */
    public String findAttachmentUID() {
        // Set attachment UID if there is one
        if (feed == null)
            return getAttachmentUID();

        // Check if we can or need to find the attachment UID
        String hash = getHash();
        String attUID = getAttachmentUID();
        if (FileSystemUtils.isEmpty(hash) || !FileSystemUtils.isEmpty(attUID))
            return getAttachmentUID();

        // Find and set
        FeedContent content = feed.getContentWithAttachment(getHash());
        if (content != null)
            setAttachmentUID(content.getUID());

        return getAttachmentUID();
    }

    @Override
    public long getEstimatedFileSize() {
        return this.size;
    }

    public long getSize() {
        return this.size;
    }

    public ResourceFile.MIMEType getMimeType() {
        ResourceFile.MIMEType mt = ResourceFile.getMIMETypeForMIME(mimeType);
        if (mt == null)
            mt = ResourceFile.getMIMETypeForFile(name);
        return mt;
    }

    public String getContentType() {
        return this.contentType;
    }

    @Override
    public void setLocalHashtags(HashtagSet tags) {
        super.setLocalHashtags(tags);
        // XXX - This modifies file hash which triggers a file publish
        // Not sure if we want this behavior
        /*if (isImage()) {
            MapView mv = MapView.getMapView();
            File file = getLocalFile();
            if (mv == null || !FileSystemUtils.isFile(file))
                return;
            AttachmentContent content = new AttachmentContent(mv, file);
            content.setHashtags(tags);
        }*/
    }

    @Override
    public boolean download() {
        return false;
    }

    @Override
    public boolean removeLocalContent() {
        File f = getLocalFile();
        FileChangeWatcher.getInstance().ignore(f);
        if (!super.removeLocalContent() && f != null)
            FileSystemUtils.delete(f);
        FileChangeWatcher.getInstance().unignore(f);
        return true;
    }

    @Override
    public boolean contentEquals(FeedContent o) {
        if (o instanceof FeedFile) {
            FeedFile file = (FeedFile) o;
            return Objects.equals(this.hash, file.hash);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FeedFile that = (FeedFile) o;
        return this.size == that.size &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.mimeType, that.mimeType) &&
                Objects.equals(this.contentType, that.contentType) &&
                Objects.equals(this.uid, that.uid) &&
                Objects.equals(this.hash, that.hash) &&
                Objects.equals(this.localPath, that.localPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.name, this.mimeType,
                this.contentType, this.uid, this.hash, this.size,
                this.localPath);
    }

    public boolean isImage() {
        // File is definitely an image
        if (FileUtils.isImage(getLocalFile()))
            return true;

        // Check if file is a NITF created with Image Markup
        return FileSystemUtils.isEquals(contentType, "Image Markup");
    }

    public boolean isTextFile() {
        return FileUtils.isTextFile(new File(name));
    }

    public boolean isFeatureSet() {
        return FileUtils.isFeatureSet(new File(name));
    }

    public boolean isFileDatabase() {
        return FileUtils.isFileDatabase(new File(name));
    }

    public boolean isGRG() {
        return FileSystemUtils.isEquals(contentType,
                GRGMapComponent.IMPORTER_CONTENT_TYPE);
    }

    public boolean isVideo() {
        File f = getLocalFile();
        return FileUtils.isVideo(f != null ? f : new File(name));
    }

    @Override
    public boolean goTo(boolean select) {

        File file = getLocalFile();
        if (!FileSystemUtils.isFile(file))
            return false;

        // Handler re-direct - do not use the return value as a control
        // for whether we should continue execution
        URIContentHandler handler = URIContentManager.getInstance()
                .getHandler(file);
        if (handler != null && handler.isActionSupported(GoTo.class)) {
            ((GoTo) handler).goTo(select);
            return true;
        }

        if (isImage()) {
            Intent show = new Intent(ImageDropDownReceiver.IMAGE_DISPLAY);
            show.putExtra("imageURI", URIHelper.getURI(file));

            String creator = getCreatorName();
            if (!FileSystemUtils.isEmpty(creator))
                show.putExtra("title", "Created by: " + creator);

            GeoPoint point = getPoint(null);

            // Focus on attached map item
            MapItem attachedTo = FeedUtils.findMapItem(getAttachmentUID());
            if (attachedTo != null) {
                String uid = attachedTo.getUID();
                AtakBroadcast.getInstance().sendBroadcast(new Intent(
                        FocusBroadcastReceiver.FOCUS)
                        .putExtra("uid", uid)
                        .putExtra("useTightZoom", true));
                AtakBroadcast.getInstance().sendBroadcast(new Intent(
                        CoordOverlayMapReceiver.SHOW_DETAILS)
                        .putExtra("uid", uid));
                show.putExtra("uid", uid);
            } else if (point != null) {
                AtakBroadcast.getInstance().sendBroadcast(new Intent(
                        "com.atakmap.android.maps.ZOOM_TO_LAYER")
                        .putExtra("point", point.toString()));
            }
            AtakBroadcast.getInstance().sendBroadcast(show);
        } else if (isVideo()) {
            // View the video within ATAK
            Intent i = new Intent(VideoDropDownReceiver.DISPLAY);
            i.putExtra("CONNECTION_ENTRY", new ConnectionEntry(file));
            AtakBroadcast.getInstance().sendBroadcast(i);
        } else if (isImportable()) {
            // Import the file if it isn't already
            importFile(true);
        } else {
            // Default behavior
            MapView mv = MapView.getMapView();
            if (mv != null)
                MIMETypeMapper.openFile(file, mv.getContext());
        }
        return true;
    }

    /**
     * Attempt to import this file into ATAK
     * @param userTriggered True if this action was triggered by the user
     *                      False if automatically triggered
     * @return True if import intent sent successfully, false if cannot import
     */
    public boolean importFile(boolean userTriggered) {
        // First check if the local file exists
        File f = getLocalFile();
        if (f == null || !f.exists()) {
            if (userTriggered)
                showMessage(R.string.import_cancelled,
                        R.string.file_not_exist, getTitle());
            return false;
        }

        // Skip images
        if (isImage())
            return false;

        if (contentType != null && !contentType.equals("Other XML")) {
            ImportResolver sorter = FileUtils.findSorter(f, contentType);
            if (sorter != null) {
                sorter.beginImport(f);
                return true;
            }
        }

        // First check if the file can be imported
        if (!isImportable()) {
            if (userTriggered)
                showMessage(R.string.import_cancelled,
                        R.string.file_not_supported, getTitle());
            return false;
        }

        // APK install
        ResourceFile.MIMEType mt = getMimeType();
        if (mt == ResourceFile.MIMEType.APK) {
            // Do not auto-install APKs (prompts the user unexpectedly)
            if (!userTriggered)
                return false;
            MapView mv = MapView.getMapView();
            if (mv != null)
                AppMgmtUtils.install(mv.getContext(), f);
        }

        // Use Import Manager to import data
        AtakBroadcast.getInstance().sendBroadcast(new Intent(
                ImportExportMapComponent.USER_HANDLE_IMPORT_FILE_ACTION)
                .putExtra("filepath", f.getAbsolutePath())
                .putExtra("promptOnMultipleMatch", userTriggered)
                .putExtra("importInPlace", true));
        return true;
    }

    /**
     * Check if this mission file can be imported into ATAK by performing
     * some checks against the file and metadata
     * @return True if importable
     */
    public boolean isImportable() {
        // File has to exist in order to be imported
        File f = getLocalFile();
        if (!FileSystemUtils.isFile(f))
            return false;

        // Do not import image files from mission
        // Otherwise we get some dynamically created quick-pic marker that
        // just makes things a headache to manage
        if (isImage())
            return false;

        // Check if the file has a registered content type
        if (contentType != null && !contentType.equals("Other XML"))
            return true;

        // Finally check if any sorters support the file
        return !FileUtils.findSorters(f).isEmpty();
    }

    /**
     * Show a generic alert dialog for this file
     * @param titleId Dialog title string id
     * @param msgId Message string id
     * @param args Message arguments
     */
    protected void showMessage(int titleId, int msgId, Object... args) {
        FeedUtils.toast(msgId, args);
    }

    protected FeedFile(Feed feed, XMLMissionFile mf) {
        super(feed, mf);

        this.localPath = mf.localPath;
        this.attachmentUID = mf.attachmentUid;

        XMLContentResourceData data = mf.metadata;
        if (data != null) {
            this.name = data.name;
            this.mimeType = data.mimeType;
            this.contentType = data.contentType;
            this.creatorUID = data.creatorUid;
            this.uid = data.uid;
            this.hash = data.hash;

            XMLCreatorData creatorData = data.creatorData;
            if (creatorData != null)
                setTimestamp(creatorData.timestamp);

            String[] kwArr = data.keywords;
            String kwString = data.keywordString;
            if (kwArr == null && !FileSystemUtils.isEmpty(kwString))
                kwArr = kwString.split(",");
            if (kwArr != null)
                setHashtags(Arrays.asList(kwArr));
        }
    }
}
