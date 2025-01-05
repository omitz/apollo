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

import com.atakmap.android.missionapi.interfaces.Unread;
import com.atakmap.android.missionapi.model.json.FeedChange.Type;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for handling feed change data
 */
public class FeedChangeManager implements Unread {

    private static final String TAG = "MissionChangeManager";

    private final Feed _feed;

    // All the changes for this mission sorted by time
    // The map value is whether this change is server-side (true) or local (false)
    private final FeedChangeMap _changes = new FeedChangeMap();

    // Changes per key (content UID)
    private final Map<String, FeedChangeMap> _keyChanges = new HashMap<>();

    public FeedChangeManager(Feed feed) {
        _feed = feed;
    }

    /**
     * Get list of mission changes
     *
     * @return List of mission changes (sorted by time)
     */
    public synchronized List<FeedChange> getChanges() {
        return _changes.getSortedList();
    }

    /**
     * Get the first change in the feed (the mission created change)
     * @return Feed change
     */
    public synchronized FeedChange getFirstChange() {
        return _changes.getFirst();
    }

    /**
     * Get the latest change in the feed
     * @return Latest change
     */
    public synchronized FeedChange getLatestChange() {
        return _changes.getLast();
    }

    /**
     * Get all changes for a given mission item
     *
     * @param mc Mission content
     * @return List of related changes (sorted by time)
     */
    public List<FeedChange> getChanges(FeedContent mc) {
        return mc != null ? getChanges(mc.getUID())
                : new ArrayList<FeedChange>();
    }

    /**
     * Get all changes that match the given key
     *
     * @param key Content key to match
     * @return List of matching changes
     */
    public synchronized List<FeedChange> getChanges(String key) {
        FeedChangeMap changes = _keyChanges.get(key);
        return changes != null ? changes.getSortedList()
                : new ArrayList<FeedChange>();
    }

    /**
     * Check if the change is the first change in the mission for the CoT/UID or file/hash
     *
     * @param change Mission change
     * @return True if it's the first change
     */
    public boolean isFirstChange(FeedChange change) {
        if (change == null || !change.isValid())
            return false;

        String key = change.getContentUID();
        return change.equals(getFirstChange(key));
    }

    /**
     * Check if this ADD_CONTENT change is either the initial add or
     * first add after a remove
     *
     * @param change ADD_CONTENT change
     * @return True if its the first add
     */
    public boolean isFirstAdd(FeedChange change) {
        if (change == null || !change.isValid()
                || change.type != Type.ADD_CONTENT)
            return false;

        String key = change.getContentUID();

        boolean foundSame = false;
        List<FeedChange> changes = getChanges(key);
        for (FeedChange c : changes) {
            if (c.equals(change)) {
                foundSame = true;
            } else if (foundSame) {
                if (c.type == Type.ADD_CONTENT)
                    return false;
                else if (c.type == Type.REMOVE_CONTENT)
                    return true;
            }
        }
        return true;
    }

    /**
     * Get first change log entry for the specified CoT/UID
     *
     * @param cot Mission CoT
     * @return Change data
     */
    public FeedChange getFirstChange(FeedCoT cot) {
        if (cot == null || !cot.isValid())
            return null;

        return getFirstChange(cot.getUID());
    }

    /**
     * Get first change log entry for the specified file/hash
     *
     * @param file Mission file
     * @return Change data
     */
    public FeedChange getFirstChange(FeedFile file) {
        if (file == null || !file.isValid())
            return null;

        return getFirstChange(file.hash);
    }

    public synchronized FeedChange getFirstChange(String key) {
        FeedChangeMap changes = _keyChanges.get(key);
        return changes != null ? changes.getLast() : null;
    }

    public synchronized FeedChange getLatestChange(String key) {
        FeedChangeMap changes = _keyChanges.get(key);
        return changes != null ? changes.getFirst() : null;
    }

    /**
     * Check if the change is the last change in the mission for the CoT/UID or file/hash
     *
     * @param change Mission change
     * @return True if it's the last change
     */
    public boolean isLastChange(FeedChange change) {
        if (change == null || !change.isValid())
            return false;
        return change.equals(getLatestChange(change.getContentUID()));
    }

    /**
     * Merge list of changes from server or database with current change cache
     *
     * @param changes List of mission changes
     * @param markUnread True if new changes should be marked as unread
     * @param persist Persist changes
     * @return The latest change
     */
    public FeedChange addChanges(List<FeedChange> changes, boolean markUnread,
                                 boolean persist) {
        if (FileSystemUtils.isEmpty(changes)) {
            //Log.w(TAG, "No changes for mission: " + _feed.uid);
            return null;
        }

        Log.d(TAG, "mergeChanges for mission: " + _feed.uid + " "
                + changes.size() + " total");
        Map<String, List<FeedChange>> keyMap = new HashMap<>();
        for (int i = 0; i < changes.size(); i++) {
            FeedChange change = changes.get(i);
            if (change == null || !change.isValid()) {
                Log.w(TAG, "invalid change: " + change);
                changes.remove(i--);
                continue;
            }

            // Add entry change key -> change list
            keyChange(change, change.getContentUID(), keyMap);

            // XXX - YUCK - Server has both UID and hash for files
            // But we can't make our minds up on which data point we should use
            if (change instanceof FeedFileChange)
                keyChange(change, ((FeedFileChange) change).details.uid, keyMap);

            change.feed = _feed;
        }
        FeedChange newest = null;
        synchronized (this) {
            List<FeedChange> newChanges = _changes.addAll(changes);
            if (!_changes.isEmpty())
                newest = _changes.getFirst();

            // Merge key change map
            for (Map.Entry<String, List<FeedChange>> e
                    : keyMap.entrySet()) {
                String k = e.getKey();
                List<FeedChange> v = e.getValue();
                FeedChangeMap kc = _keyChanges.get(k);
                if (kc == null)
                    _keyChanges.put(k, new FeedChangeMap(v));
                else
                    kc.addAll(v);
            }

            // Mark new changes as unread
            if (markUnread) {
                for (FeedChange change : newChanges) {
                    if (!change.isSelfMade())
                        change.setUnread(true);
                }
            }
        }
        if (persist)
            _feed.persist();
        return newest;
    }

    private void keyChange(FeedChange change, String key, Map<String, List<FeedChange>> keyMap) {
        List<FeedChange> value = keyMap.get(key);
        if (value == null) {
            value = new ArrayList<>();
            keyMap.put(key, value);
        }
        value.add(change);
    }

    /**
     * Remove changes from this mission
     *
     * @param changes List of changes to remove
     * @param localOnly True to only remove local (usually offline) changes
     *                  These are identified by a missing "serverTime" attribute
     *                  since they haven't been acknowledged by the server
     */
    public void removeChanges(List<FeedChange> changes,
                                           boolean localOnly) {
        synchronized (this) {
            for (FeedChange ch : changes) {
                FeedChange existing = _changes.get(ch);
                if (existing == null)
                    continue;
                if (!localOnly || existing.serverTimestamp == null)
                    removeChangeNoSync(ch);
            }
        }
        _feed.persist();
    }

    private void removeChangeNoSync(FeedChange change) {
        if (change == null)
            return;
        _changes.remove(change);
        String key = change.getContentUID();
        FeedChangeMap kc = _keyChanges.get(key);
        if (kc != null) {
            kc.remove(change);
            if (kc.isEmpty())
                _keyChanges.remove(key);
        }
    }

    /**
     * Clear all changes
     */
    public synchronized void clear() {
        _changes.clear();
        _keyChanges.clear();
    }

    @Override
    public int getUnreadCount() {
        int total = 0;
        for (FeedChange ch : getChanges()) {
            if (ch.isUnread())
                total++;
        }
        return total;
    }

    /**
     * Mark changes as read
     */
    public void markAllRead() {
        for (FeedChange ch : getChanges())
            ch.setUnread(false);
    }
}
