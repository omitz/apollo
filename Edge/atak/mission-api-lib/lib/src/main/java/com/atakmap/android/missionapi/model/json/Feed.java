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

import com.atakmap.android.missionapi.data.AuthManager;
import com.atakmap.android.missionapi.data.FeedPublishManager;
import com.atakmap.android.missionapi.data.TAKServerVersion;
import com.atakmap.android.missionapi.interfaces.Timestamp;
import com.atakmap.android.missionapi.interfaces.Title;
import com.atakmap.android.missionapi.model.json.user.UserPermission;
import com.atakmap.android.missionapi.model.json.user.UserRole;
import com.atakmap.android.missionapi.model.xml.XMLExternalData;
import com.atakmap.android.missionapi.model.xml.XMLMissionCoT;
import com.atakmap.android.missionapi.model.xml.XMLMissionConfig;
import com.atakmap.android.missionapi.model.xml.XMLMissionFile;
import com.atakmap.android.missionapi.model.xml.XMLMissionManifest;
import com.atakmap.android.missionapi.model.xml.XMLServerConfig;
import com.atakmap.android.missionapi.model.xml.XMLUserLog;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.put.PutFeedSubscriptionRequest;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.android.missionapi.util.TimeUtils;
import com.atakmap.android.hashtags.util.HashtagSet;
import com.atakmap.android.hashtags.util.HashtagUtils;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.visibility.VisibilityManager;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.TAKServerListener;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.filesystem.HashingUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All encompassing class for mission/feed data
 *
 * For instantiating feeds and their contents, please use:
 * {@link #create(Object...)}
 * {@link #createCoT(Object...)}
 * {@link #createFile(Object...)}
 * {@link #createLog(Object...)}
 * {@link #createExternalData(Object...)}
 *
 * These methods utilize the SPI system, which is used by plugins to override
 * certain behavior
 */
public class Feed implements JSONSerializable, Title, Timestamp {

    protected static final String TAG = "DataSyncFeed";

    public static final String TOOL_PUBLIC = "public";
    public static final String GROUP_ANON = "__ANON__";

    public static final int STATUS_OK = 0,
            STATUS_PROGRESS = 1,
            STATUS_INCOMPLETE = 2,
            STATUS_NA = 3;

    public String uid;
    public String name;
    public String serverNCS;
    public String description;
    public String chatRoom;
    public String tool = TOOL_PUBLIC;
    public String creatorUID;
    public String token;
    public long createTime;
    public long lastSyncTime, lastAccessTime;
    public UserRole defaultRole = UserRole.SUBSCRIBER;
    public boolean passwordProtected;
    private boolean visible = true;
    protected boolean subscribed;
    public boolean notifications = true;
    protected boolean autoPublish;
    protected boolean deleted;

    private final List<String> groups = new ArrayList<>();
    private final HashtagSet hashtags = new HashtagSet();
    public final FeedChangeManager changes = new FeedChangeManager(this);
    private final FeedPublishManager publisher = new FeedPublishManager(this);

    // Contents
    private final List<FeedCoT> cot = new ArrayList<>();
    private final List<FeedFile> files = new ArrayList<>();
    private final List<FeedExternalData> extData = new ArrayList<>();
    private final List<FeedLog> logs = new ArrayList<>();
    private final Map<String, FeedContent> contentMap = new HashMap<>();

    /**
     * Create feed from server-side JSON format
     * @param data JSON data
     */
    protected Feed(JSONData data) {
        this.name = data.get("name");
        this.uid = data.get("uid");
        this.serverNCS = data.get("server");
        this.description = data.get("description");
        this.chatRoom = data.get("chatRoom");
        this.tool = data.get("tool", TOOL_PUBLIC);
        setGroups(data.getList("groups", GROUP_ANON));
        setHashtags(data.getList("keywords", ""));
        this.creatorUID = data.get("creatorUid");
        this.createTime = data.getTimestamp("createTime");
        this.defaultRole = UserRole.fromJSON((Object) data.get("defaultRole"));
        this.passwordProtected = data.get("passwordProtected", false);
        this.token = data.get("token");
        this.visible = data.get("visible", true);
        this.notifications = data.get("notifications", true);
        this.subscribed = data.get("subscribed", false);
        this.autoPublish = data.get("autoPublish", false);
        this.lastSyncTime = data.get("lastSyncTime", getCreationTime());
        this.lastAccessTime = data.get("lastAccessTime", getCreationTime());

        List<FeedContent> contents = new ArrayList<>();
        contents.addAll(data.getList("uids", this, FeedCoT.class));
        contents.addAll(data.getList("contents", this, FeedFile.class));
        contents.addAll(data.getList("externalData", this, FeedExternalData.class));
        contents.addAll(data.getList("logs", this, FeedLog.class));
        addContents(contents);

        changes.addChanges(data.getList("changes", this, FeedChange.class),
                false, false);
    }

    protected Feed(String serverNCS, String name) {
        this.name = name;
        this.serverNCS = serverNCS;
        this.uid = FeedUtils.createUid(serverNCS, name);
        this.creatorUID = MapView.getDeviceUid();
        this.createTime = new CoordinatedTime().getMilliseconds();
    }

    /**
     * Convert feed to JSON data
     * @param server True if this data will be sent to the server
     * @return JSON data
     */
    @Override
    public JSONData toJSON(boolean server) {
        // Feed metadata
        JSONData o = new JSONData(server);
        o.set("name", this.name);
        o.set("uid", this.uid);
        o.set("server", this.serverNCS);
        o.set("description", this.description);
        o.set("chatRoom", this.chatRoom);
        o.set("tool", this.tool);
        o.set("groups", this.groups);
        o.set("keywords", this.hashtags);
        o.set("creatorUid", this.creatorUID);
        o.set("createTime", TimeUtils.getTimestamp(this.createTime));
        o.set("defaultRole", this.defaultRole.toServerString());
        o.set("passwordProtected", this.passwordProtected);
        o.set("visible", this.visible);
        o.set("notifications", this.notifications);
        o.set("subscribed", isSubscribed());
        o.set("autoPublish", this.autoPublish);
        o.set("lastSyncTime", getLastSyncTime());
        o.set("lastAccessTime", getLastAccessTime());

        // Changes
        o.set("changes", changes.getChanges());

        // Contents
        o.set("uids", getCoT());
        o.set("contents", getFiles());
        o.set("externalData", getExternalData());
        o.set("logs", getLogs());

        return o;
    }

    /**
     * Update this feed based on new feed data using the ID
     * @param other New feed
     * @param fromServer True if the other feed is from the server
     */
    public void update(Feed other, boolean fromServer) {
        // Feed must have the same UID
        if (other == null || !other.getUID().equals(getUID()))
            return;

        // Update metadata
        this.description = other.getDescription();
        this.chatRoom = other.getChatRoom();
        this.tool = other.tool;
        setGroups(other.getGroups());
        setHashtags(other.getHashtags(), true);
        this.creatorUID = other.getCreatorUID();
        this.createTime = other.getCreationTime();
        this.defaultRole = other.getDefaultRole();
        this.passwordProtected = other.isPasswordProtected();
        this.token = other.token;

        if (!fromServer) {
            // Notifications, visibility, and subscription state are local
            this.visible = other.visible;
            this.notifications = other.notifications;
            this.subscribed = other.subscribed;
        }

        // Update contents
        updateContents(other.getContents(), fromServer);

        // Server-side feed struct does not include changes
    }

    /**
     * Check if feed is valid
     * @return True if valid
     */
    public boolean isValid() {
        return !FileSystemUtils.isEmpty(getUID());
    }

    /**
     * Get feed UID
     * @return Feed UID
     */
    public String getUID() {
        return this.uid;
    }

    /**
     * Get the name of this feed
     * @return Name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get description of this feed
     * @return Description
     */
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) { this.description = description; }


    /**
     * Full title containing description -  used for UI display
     * @return Full title + description
     */
    @Override
    public String getTitle() {
        String name = getName();
        String desc = getDescription();
        return !FileSystemUtils.isEmpty(desc) ? (name + " - " + desc) : name;
    }

    /**
     * Get a description for the last update time, content count, and total size
     * @return Content description
     */
    public String getContentDescription() {
        return "";
    }

    /**
     * Get the creator UID
     * @return Creator UID
     */
    public String getCreatorUID() {
        return this.creatorUID;
    }

    /**
     * Get the creator name
     * @return Creator name
     */
    public String getCreatorName() {
        return FeedUtils.getUserCallsign(getCreatorUID(), this);
    }

    /**
     * Get the chat room this feed uses
     * @return Chat room
     */
    public String getChatRoom() {
        return this.chatRoom;
    }

    public void setChatRoom(String chatRoom) { this.chatRoom = chatRoom; }

    /**
     * Get default user role
     * @return User role
     */
    public UserRole getDefaultRole() {
        return this.defaultRole;
    }

    /**
     * Get the mission status code
     * @return Integer representing mission status
     */
    public int getStatusCode() {
        return STATUS_OK;
    }

    /**
     * Get icon ID
     * @return Icon ID
     */
    public int getIconId() {
        return 0;
    }

    /**
     * Get icon drawable
     * @return Icon drawable
     */
    public Drawable getIconDrawable() {
        return null;
    }

    /**
     * Set server connect string
     * @param ncs Net connect string
     */
    public void setServer(NetConnectString ncs) {
        this.serverNCS = ncs.toString();
        this.uid = FeedUtils.createUid(ncs, this.name);
    }

    /**
     * Get server URL this feed is part of
     * @return Server URL
     */
    public String getServerURL() {
        return FeedUtils.NCStoURL(this.serverNCS);
    }

    /**
     * Get server connect string
     * @return Server connect string
     */
    public String getServerConnectString() {
        return this.serverNCS;
    }

    /**
     * Get TAK server
     * @return Feed server
     */
    public TAKServer getServer() {
        return TAKServerListener.getInstance().findServer(this.serverNCS);
    }

    /**
     * Get server version
     * @return TAK server version
     */
    public TAKServerVersion getServerVersion() {
        TAKServer server = getServer();
        return server != null ? new TAKServerVersion(server) : null;
    }

    /**
     * Check if the user is connected to this feed
     * @return True if feed is connected
     */
    public boolean isConnected() {
        TAKServer server = getServer();
        return server != null && server.isConnected();
    }

    /**
     * Check if this feed has been removed from the server, and therefore
     * should NOT be persisted
     * @return True if removed from the server
     */
    public boolean removedFromServer() {
        return this.deleted;
    }

    public void setRemovedFromServer(boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Set feed hashtags
     * @param tags Hashtags list
     * @param updateContents Update content hashtags
     */
    public void setHashtags(HashtagSet tags, boolean updateContents) {
        HashtagSet removed;
        synchronized (hashtags) {
            removed = new HashtagSet(this.hashtags);
            removed.removeAll(tags);
            this.hashtags.clear();
            for (String tag : tags) {
                tag = HashtagUtils.validateTag(tag);
                this.hashtags.add(tag);
            }
            /*if (inDatabase())
                Log.d(TAG, "Set " + getName() + " tags to " + getHashtagsString(), new Throwable());*/
        }
        if (updateContents) {
            for (FeedContent c : getContents()) {
                HashtagSet cTags = new HashtagSet(c.getHashtags());
                cTags.removeAll(removed);
                cTags.addAll(tags);
                c.setHashtags(cTags);
            }
        }
    }

    public void setHashtags(HashtagSet tags) {
        setHashtags(tags, false);
    }

    public void setHashtags(List<String> tags) {
        setHashtags(new HashtagSet(tags));
    }

    /**
     * Set hashtags based on string
     * @param tags Hashtags string
     */
    public void setHashtags(String tags) {
        setHashtags(HashtagUtils.extractTags(tags));
    }

    /**
     * Get hashtags in this feed
     * @return Hashtags list
     */
    public HashtagSet getHashtags() {
        synchronized (this.hashtags) {
            return new HashtagSet(this.hashtags);
        }
    }

    public boolean hasTags() {
        synchronized (this.hashtags) {
            return !this.hashtags.isEmpty();
        }
    }

    /**
     * Get # separated list of tags (keywords)
     * @return Hashtags string
     */
    public String getHashtagsString() {
        return StringUtils.getHashtagsString(getHashtags());
    }

    /**
     * Set the list of feed groups
     * @param groups List of groups
     */
    public void setGroups(Collection<String> groups) {
        synchronized (this.groups) {
            this.groups.clear();
            if (groups.isEmpty())
                this.groups.add(GROUP_ANON);
            else
                this.groups.addAll(groups);
        }
    }

    public List<String> getGroups() {
        synchronized (this.groups) {
            return new ArrayList<>(this.groups);
        }
    }

    /**
     * Subscribe/unsubscribe to the feed
     * @param subscribed True if subscribed
     */
    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public boolean isSubscribed() {
        return this.subscribed;
    }

    /**
     * Set whether the content in this feed is visible or not
     * @param visible True if visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        VisibilityManager.getInstance().refreshConditions();
    }

    public boolean isVisible() {
        return this.visible;
    }

    /**
     * Check if a feed is password protected
     * @return Password protected
     */
    public boolean isPasswordProtected() {
        return this.passwordProtected;
    }

    /**
     * Set whether new content should be automatically published to this feed
     * @param autoPublish True to enable
     */
    public void setAutoPublish(boolean autoPublish) {
        this.autoPublish = autoPublish;
    }

    /**
     * Check if auto-publish is enabled
     * @return True to enable auto-publish
     */
    public boolean isAutoPublish() {
        return this.autoPublish && isSubscribed()
                && checkPermission(UserPermission.WRITE);
    }

    /**
     * Get publish helper for this feed
     * @return Feed publisher
     */
    public FeedPublishManager getPublisher() {
        return this.publisher;
    }

    /**
     * Persist feed to database
     * @param refresh True to call refresh
     * @param onlyIfExists True to only persist this feed if it already exists in the DB
     */
    public void persist(boolean refresh, boolean onlyIfExists) {
    }

    public void persist(boolean refresh) {
        persist(refresh, true);
    }

    public void persist() {
        persist(true);
    }

    /**
     * Refresh feed details
     */
    public void refresh() {
    }

    /**
     * Check if the feed is stored locally
     * @return True if in database
     */
    public boolean isStoredLocally() {
        return true;
    }

    /**
     * Add content to feed
     * @param content Content
     */
    public synchronized void addContent(FeedContent content) {
        addContentNoSync(content);
    }

    /**
     * Add contents to feed
     * @param contents List of contents
     */
    public synchronized void addContents(List<? extends FeedContent> contents) {
        if (contents == null)
            return;
        for (FeedContent content : contents)
            addContentNoSync(content);
    }

    private void addContentNoSync(FeedContent content) {
        // Generic UID association
        String uid = content.getUID();
        if (FileSystemUtils.isEmpty(uid))
            return;

        // Update existing instance
        boolean added = false;
        FeedContent existing = this.contentMap.get(uid);
        if (existing != content) {
            if (existing != null)
                removeContentNoSync(uid);
            added = true;
        }

        this.contentMap.put(uid, content);

        // Associate file path and hash
        if (content instanceof FeedFile) {
            // Associate files by hash and local path too
            FeedFile file = (FeedFile) content;
            String path = file.getLocalPath();
            if (!FileSystemUtils.isEmpty(path))
                this.contentMap.put(path, file);
            String hash = file.hash;
            if (!FileSystemUtils.isEmpty(hash))
                this.contentMap.put(hash, file);
        }

        if (added) {
            if (content instanceof FeedFile)
                this.files.add((FeedFile) content);
            else if (content instanceof FeedCoT)
                this.cot.add((FeedCoT) content);
            else if (content instanceof FeedExternalData)
                this.extData.add((FeedExternalData) content);
            else if (content instanceof FeedLog)
                this.logs.add((FeedLog) content);
        }

        content.feed = this;
    }

    /**
     * Remove content from feed
     * @param content Content
     */
    public synchronized FeedContent removeContent(FeedContent content) {
        return removeContentNoSync(content);
    }

    public synchronized FeedContent removeContent(String uid) {
        return removeContentNoSync(uid);
    }

    /**
     * Remove contents from feed
     * @param contents List of contents
     */
    public synchronized void removeContents(List<FeedContent> contents) {
        for (FeedContent content : contents)
            removeContentNoSync(content);
    }

    private FeedContent removeContentNoSync(String uid) {
        FeedContent content = this.contentMap.remove(uid);
        if (content instanceof FeedFile) {
            FeedFile file = (FeedFile) content;
            this.contentMap.remove(file.getLocalPath());
            this.contentMap.remove(file.hash);
            this.files.remove(content);
        } else if (content instanceof FeedCoT)
            this.cot.remove(content);
        else if (content instanceof FeedExternalData)
            this.extData.remove(content);
        else if (content instanceof FeedLog)
            this.logs.remove(content);
        return content;
    }

    private FeedContent removeContentNoSync(FeedContent content) {
        return content != null ? removeContentNoSync(content.getUID()) : null;
    }

    /**
     * Update feed file path association
     * Should only be called from the file class BEFORE updating the path
     * @param file Feed file
     */
    synchronized void updateFilePath(FeedFile file, String newPath) {
        if (!hasContent(file.hash))
            return;
        String curPath = file.getLocalPath();
        if (curPath != null) {
            FeedContent existing = this.contentMap.get(curPath);
            if (existing == file)
                this.contentMap.remove(curPath);
        }
        this.contentMap.put(newPath, file);
    }

    /**
     * Check if a feed has content with the matching UID
     * @param uid UID
     * @return True if feed has this content
     */
    public synchronized boolean hasContent(String uid) {
        return this.contentMap.containsKey(uid);
    }

    /**
     * Get content by UID
     * @param uid Content UID
     * @return Feed content
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends FeedContent> T getContent(String uid) {
        FeedContent content = this.contentMap.get(uid);
        try {
            return (T) content;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get full list of contents
     * @return List of contents
     */
    public synchronized List<FeedContent> getContents() {
        List<FeedContent> contents = new ArrayList<>(getContentCount());
        contents.addAll(this.cot);
        contents.addAll(this.files);
        contents.addAll(this.extData);
        contents.addAll(this.logs);
        return contents;
    }

    /**
     * Get set of content keys used for this feed
     * @return Content keys set
     */
    public synchronized Set<String> getContentKeys() {
        return new HashSet<>(this.contentMap.keySet());
    }

    /**
     * Update contents from another list of contents
     * @param contents List of contents
     * @param fromServer True if the contents list is from server
     */
    public synchronized void updateContents(List<FeedContent> contents, boolean fromServer) {
        List<String> toRemove = new ArrayList<>();
        for (FeedContent content : getContents()) {
            if (!fromServer || !(content instanceof FeedLog))
                toRemove.add(content.getUID());
        }

        // Add/update contents
        for (FeedContent content : contents) {
            FeedContent existing = this.contentMap.get(content.getUID());
            if (existing != null)
                existing.update(content, fromServer);
            else
                addContentNoSync(content);
            toRemove.remove(content.getUID());
        }

        // Remove contents that are no longer in the feed
        for (String uid : toRemove)
            removeContentNoSync(uid);
    }

    /**
     * Get content count
     * @return Content count total
     */
    public synchronized int getContentCount() {
        return this.cot.size() + this.files.size() + this.extData.size()
                + this.logs.size();
    }

    /**
     * Get list of CoT in the feed
     * @return List of CoT
     */
    public synchronized List<FeedCoT> getCoT() {
        return new ArrayList<>(this.cot);
    }

    public synchronized int getCoTCount() {
        return this.cot.size();
    }

    /**
     * Get list of files in the feed
     * @return List of files
     */
    public synchronized List<FeedFile> getFiles() {
        return new ArrayList<>(this.files);
    }

    public synchronized int getFileCount() {
        return this.files.size();
    }

    /**
     * Get list of external data in the feed
     * @return List of external data
     */
    public synchronized List<FeedExternalData> getExternalData() {
        return new ArrayList<>(this.extData);
    }

    /**
     * Get list of logs in the feed
     * @return List of feed logs
     */
    public synchronized List<FeedLog> getLogs() {
        return new ArrayList<>(this.logs);
    }

    public synchronized int getLogCount() {
        return this.logs.size();
    }

    /**
     * Get list of files attached to a map item
     * @param uid Map item UID
     * @return List of files
     */
    public synchronized List<FeedFile> getAttachedFiles(String uid) {
        List<FeedFile> attachments = new ArrayList<>();
        if (FileSystemUtils.isEmpty(uid))
            return attachments;

        for (FeedFile f : this.files) {
            if (FileSystemUtils.isEquals(uid, f.attachmentUID))
                attachments.add(f);
        }

        return attachments;
    }

    /**
     * Get content with attachment
     * @param hash File hash
     * @return Feed content or null if not found
     */
    public synchronized FeedContent getContentWithAttachment(String hash) {
        if (FileSystemUtils.isEmpty(hash))
            return null;

        for (FeedCoT c : getCoT()) {
            if (c.hasAttachmentHash(hash))
                return c;
        }

        for (FeedLog c : getLogs()) {
            if (FeedUtils.contains(c.getEntryHashes(), hash))
                return c;
        }

        return null;
    }

    /**
     * Call cache() method on all contents
     */
    public void cacheContents() {
        for (FeedContent c : getContents())
            c.cache();
    }

    /**
     * Get all changes associated with this feed
     * @return List of changes
     */
    public List<FeedChange> getChanges() {
        return changes.getChanges();
    }

    /**
     * Convert changes to feed content
     * @param changes List of changes to process
     * @param markUnread Mark new changes unread
     */
    public void addChanges(List<FeedChange> changes, boolean markUnread) {
        if (FileSystemUtils.isEmpty(changes))
            return;

        this.changes.addChanges(changes, markUnread, false);

        Log.d(TAG, "Adding " + changes.size() + " changes for: " + uid);
        for (FeedChange change : changes) {
            if (change == null || !change.isValid()) {
                Log.w(TAG, "Cannot add empty change for: " + uid);
                continue;
            }

            if (change.getType() != FeedChange.Type.ADD_CONTENT)
                continue;

            FeedChange latest = this.changes.getLatestChange(change.getContentUID());
            if (latest != change)
                continue;

            // CoT item
            if (change instanceof FeedCoTChange) {
                FeedCoT localCot = getContent(change.getContentUID());
                FeedCoTChange cotChange = (FeedCoTChange) change;
                if (localCot != null)
                    localCot.update(cotChange);
                else {
                    localCot = (FeedCoT) cotChange.getContent();
                    addContent(localCot);
                }
            }

            // File
            else if (change instanceof FeedFileChange) {
                FeedFile localFile = getContent(change.getContentUID());
                FeedFileChange fileChange = (FeedFileChange) change;
                if (localFile != null)
                    localFile.update(fileChange);
                else {
                    FeedFile file = (FeedFile) fileChange.getContent();
                    addContent(file);
                }
            }
        }
    }

    public void addChanges(List<FeedChange> changes) {
        addChanges(changes, false);
    }

    /**
     * Get estimated size in bytes
     * @return Estimated size
     */
    public long getEstimatedFileSize() {
        long size = 0;
        for (FeedContent content : getContents())
            size += content.getEstimatedFileSize();
        return size;
    }

    /**
     * Get the data diretory for this feed
     * @return Data directory
     */
    public File getDataDir() {
        return FileSystemUtils.getItem(FeedUtils.DATA_FOLDER
                + File.separator + uid);
    }

    /**
     * Get the default file path for feed files
     * @param name File name
     * @param hash File hash (SHA-256)
     * @return Default file
     */
    public File getDefaultFilePath(String name, String hash) {
        return new File(getDataDir(), hash + File.separator + name);
    }

    public File getDefaultFilePath(FeedFile file) {
        return getDefaultFilePath(file.getTitle(), file.getHash());
    }

    public File getDefaultFilePath(File f) {
        if (!FileSystemUtils.isFile(f))
            return f;
        return getDefaultFilePath(f.getName(), HashingUtils.sha256sum(f));
    }

    /**
     * Move a list of files to the default mission directory
     * Files must be part of the mission already
     * @param files List of files to move
     * @return How many files were actually moved
     */
    public int moveToMissionDirectory(List<File> files) {
        int renamed = 0;
        for (File f : files) {
            // Check if file is part of this mission
            FeedFile mf = getContent(f.getAbsolutePath());
            if (mf != null) {
                // Move to the default directory
                File moveTo = getDefaultFilePath(mf);
                if (moveTo.equals(f))
                    continue;
                File moveToDir = moveTo.getParentFile();
                if (moveToDir != null && !moveToDir.exists()
                        && !moveToDir.mkdirs()) {
                    Log.w(TAG, "Failed to make directory: "
                            + moveToDir);
                    continue;
                }
                FileSystemUtils.renameTo(f, moveTo);
                mf.setLocalFile(moveTo);
                renamed++;
            }
        }
        return renamed;
    }

    /**
     * Check if the local user created this feed
     * @return True if the user created this feed
     */
    public boolean isSelfMade() {
        return FileSystemUtils.isEquals(MapView.getDeviceUid(), this.creatorUID);
    }

    /**
     * Check if the feed has a defined chat room
     * @return Chat room
     */
    public boolean hasChatRoom() {
        return !FileSystemUtils.isEmpty(chatRoom)
                && !chatRoom.equals(FeedUtils.NULL);
    }

    /**
     * Get local user's role in the feed
     * @return User role
     */
    public UserRole getRole() {
        if (AuthManager.hasAccessToken(this))
            return AuthManager.getRole(this);
        if (isSelfMade() && !checkSupport(TAKServerVersion.SUPPORT_ROLES))
            return UserRole.OWNER;
        return UserRole.SUBSCRIBER;
    }

    /**
     * Check if user has a permission
     * @param permission Permission
     * @return Permission integer
     */
    public boolean checkPermission(int permission) {
        return getRole().hasPermission(permission);
    }

    /**
     * Check if the server this feed is part of supports a given feature
     * @param supportVersion Feature version
     * @return True if the server supports a feature
     */
    public boolean checkSupport(TAKServerVersion supportVersion) {
        TAKServerVersion version = getServerVersion();
        return version != null && version.checkSupport(supportVersion);
    }

    /**
     * Creation timestamp
     * @return Creation time
     */
    public String getCreationTimestamp() {
        return TimeUtils.getTimestamp(getCreationTime());
    }

    public long getCreationTime() {
        return this.createTime;
    }

    /**
     * Get the time this feed was last synced with the server
     * @return Last sync time in milliseconds
     */
    public long getLastSyncTime() {
        return this.lastSyncTime;
    }

    public boolean hasLastSyncTime() {
        return this.lastSyncTime > 0;
    }

    public void setLastSyncTime(long time) {
        if (time < 0) {
            Log.w(TAG, "setLastSyncTime invalid: " + time, new Exception());
            this.lastSyncTime = -1;
            return;
        }
        this.lastSyncTime = time;
    }

    public void updateLastSyncTime() {
        setLastSyncTime(new CoordinatedTime().getMilliseconds());
    }

    @Override
    public long getTime() {
        return hasLastSyncTime() ? getLastSyncTime() : getCreationTime();
    }

    @Override
    public String getTimestamp() {
        return TimeUtils.getTimestamp(getTime());
    }

    /**
     * Get the time this mission was last accessed by the user
     * @return Last access time in milliseconds
     */
    public long getLastAccessTime() {
        return this.lastAccessTime;
    }

    public boolean hasLastAccessTime() {
        return this.lastAccessTime > 0;
    }

    public void setLastAccessTime(long access) {
        this.lastAccessTime = access;
    }

    public void updateLastAccessTime() {
        this.lastAccessTime = new CoordinatedTime().getMilliseconds();
        persist(false);
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    /**
     * Get the feed created string
     * @return Created string
     */
    public StringBuilder getCreatedString() {
        String creator = FeedUtils.getUserCallsign(getCreatorUID(), this);
        String createTime = getCreationTimestamp();
        StringBuilder overview = new StringBuilder();
        if (!FileSystemUtils.isEmpty(creator)
                || !FileSystemUtils.isEmpty(createTime)) {
            if (!FileSystemUtils.isEmpty(creator))
                overview.append(creator).append(" ");
            if (!FileSystemUtils.isEmpty(createTime))
                overview.append("@ ").append(createTime);
        }
        return overview;
    }

    /**
     * Sync to this mission
     * This should be called ANYTIME we need to join and subscribe to a mission
     * Do NOT try manually doing it or things will probably break
     */
    public void syncMission() {
        setLastSyncTime(createTime <= 0 ? 0 : createTime - 1000);
        persist(false, false);
        requestSubscribe(true);
    }

    /**
     * Called when requesting subscribe from the server
     * @param subscribed True to subscribe
     */
    public void requestSubscribe(boolean subscribed) {
        RestManager.getInstance().executeRequest(
                new PutFeedSubscriptionRequest(this, subscribed, true));
    }

    /**
     * Check if feed content is downloading
     * @return True if content in this feed is downloading
     */
    public boolean isDownloading() {
        return !RestManager.getInstance().getContentRequests(this, null)
                .isEmpty();
    }

    /**
     * Get number of contents that haven't been downloaded (aren't stored locally)
     * @return Download count
     */
    public int getDownloadCount() {
        int total = 0;
        List<FeedContent> contents = getContents();
        for (FeedContent fc : contents) {
            if (!fc.isStoredLocally())
                total++;
        }
        return total;
    }

    /**
     * Get number of unread contents
     * @return Unread count
     */
    public int getUnreadCount() {
        return getUnreadLogCount() + changes.getUnreadCount();
    }

    public int getUnreadLogCount() {
        int logsUnread = 0;
        for (FeedLog log : getLogs()) {
            if (log.isUnread())
                logsUnread++;
        }
        return logsUnread;
    }

    /**
     * Mark all content and changes read
     */
    public void markAllRead() {
        for (FeedContent c : getLogs())
            c.setUnread(false);
        changes.markAllRead();
    }

    public int getMissingContentCount() {
        int missing = 0;
        for (FeedContent c : getContents()) {
            if (c.isMissing())
                missing++;
        }
        return missing;
    }

    /**
     * Join chat room or view chat drop-down
     * @return True if drop-down shown
     */
    public boolean viewChat() {
        return false;
    }

    @Override
    public String toString() {
        return this.name + " (" + this.uid + ")";
    }

    /**
     * Create feed data from legacy XML format
     * @param xml XML struct
     */
    protected Feed(XMLMissionManifest xml) {
        // Metadata
        this.uid = xml.uid;
        this.name = xml.label;
        this.tool = TOOL_PUBLIC;
        this.visible = true;
        this.notifications = xml.notifications;
        this.lastSyncTime = xml.lastSyncTime;
        this.lastAccessTime = xml.lastAccessTime;
        XMLMissionConfig config = xml.config;
        if (config != null) {
            this.serverNCS = config.serverConnectString;
            XMLServerConfig server = config.mission;
            if (server != null) {
                this.description = server.description;
                this.chatRoom = server.chatroom;
                this.creatorUID = server.creatorUid;
                this.createTime = TimeUtils.parseTimestampMillis(
                        server.createTime);
                this.defaultRole = UserRole.fromString(server.defaultRole);
                this.passwordProtected = server.passwordProtected;
                this.subscribed = server.subscribe;
            }
            if (config.keywords != null)
                setHashtags(config.keywords);
        }

        // Contents
        List<FeedContent> contents = new ArrayList<>();
        for (XMLMissionCoT mc : xml.contents.cot)
            contents.add(createCoT(this, mc));
        for (XMLMissionFile mf : xml.contents.files)
            contents.add(createFile(this, mf));
        for (XMLExternalData data : xml.contents.externalData)
            contents.add(createExternalData(this, data));
        for (XMLUserLog log : xml.contents.userLogs)
            contents.add(createLog(this, log));
        addContents(contents);
    }

    /* SPIs */

    public static Feed create(Object... args) {
        return FeedSPI.getInstance().createFeed(args);
    }

    public static FeedCoT createCoT(Object... args) {
        return createContent(FeedCoT.class, args);
    }

    public static FeedFile createFile(Object... args) {
        return createContent(FeedFile.class, args);
    }

    public static FeedExternalData createExternalData(Object... args) {
        return createContent(FeedExternalData.class, args);
    }

    public static FeedLog createLog(Object... args) {
        return createContent(FeedLog.class, args);
    }

    public static <T extends FeedContent> T createContent(Class<T> clazz,
                                                          Object... args) {
        return FeedSPI.getInstance().createContent(clazz, args);
    }
}
