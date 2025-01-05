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

package com.atakmap.android.missionapi.data;

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages feeds by UID
 * Also contains a cached list of feeds stored locally
 */
public class FeedManager implements LocalFeedProvider {

    private static final String TAG = "FeedManager";

    protected static FeedManager _instance = new FeedManager();

    public static FeedManager getInstance() {
        return _instance;
    }

    // UID -> Feed
    private final Map<String, Feed> _feeds = new HashMap<>();

    // Listeners
    private final Set<Listener> _listeners = new HashSet<>();

    // Local feed provider
    private LocalFeedProvider _localProvider;

    public void init(LocalFeedProvider provider) {
        _localProvider = provider != null ? provider : this;
        if (provider != this) {
            List<Feed> feeds = _localProvider.getFeeds();
            addFeeds(feeds, true);
        }
    }

    /**
     * Register a feed
     * @param feed Feed to add (must have a valid UID)
     */
    public void addFeed(Feed feed) {
        synchronized (_feeds) {
            addFeedNoSync(feed);
        }
        onFeedsAdded(Collections.singletonList(feed));
    }

    /**
     * Register a list of feeds
     * @param feeds List of feeds to add
     * @param replaceExisting True to replace existing feeds
     */
    public void addFeeds(List<Feed> feeds, boolean replaceExisting) {
        synchronized (_feeds) {
            for (Feed feed : feeds) {
                if (replaceExisting || !_feeds.containsKey(feed.uid))
                    addFeedNoSync(feed);
            }
        }
        onFeedsAdded(feeds);
    }

    public void addFeeds(List<Feed> feeds) {
        addFeeds(feeds, false);
    }

    private void addFeedNoSync(Feed feed) {
        if (feed == null || FileSystemUtils.isEmpty(feed.uid)) {
            Log.d(TAG, "Failed to add invalid feed: " + feed);
            return;
        }
        _feeds.put(feed.uid, feed);
    }


    /**
     * Remove a feed from the list
     * @param feed Feed to remove
     */
    public void removeFeed(Feed feed) {
        synchronized (_feeds) {
            removeFeedNoSync(feed);
        }
        onFeedsRemoved(Collections.singletonList(feed));
    }

    public void removeFeeds() {
        List<Feed> feeds;
        synchronized (_feeds) {
            feeds = new ArrayList<>(_feeds.values());
            _feeds.clear();
        }
        onFeedsRemoved(feeds);
    }

    private void removeFeedNoSync(Feed feed) {
        if (feed == null || FileSystemUtils.isEmpty(feed.uid))
            return;
        _feeds.remove(feed.uid);
    }

    /**
     * Get a feed by its UID (derived from server URL + feed name)
     * @param uid Feed UID
     * @return Matching feed or null if not found
     */
    public Feed getFeed(String uid) {
        if (FileSystemUtils.isEmpty(uid))
            return null;
        synchronized (_feeds) {
            return _feeds.get(uid);
        }
    }

    /**
     * Get all feeds
     * @return List of feeds
     */
    @Override
    public List<Feed> getFeeds() {
        synchronized (_feeds) {
            return new ArrayList<>(_feeds.values());
        }
    }

    /**
     * Get all local subscribed feeds
     * @return List of subscribed feeds
     */
    public List<Feed> getSubscribedFeeds() {
        List<Feed> subbed = new ArrayList<>();
        for (Feed f : getLocalFeeds()) {
            if (f.isSubscribed())
                subbed.add(f);
        }
        return subbed;
    }

    /**
     * Get list of feeds stored locally via local feed provider
     * @return List of local feeds
     */
    public List<Feed> getLocalFeeds() {
        return _localProvider != null ? _localProvider.getFeeds()
                : new ArrayList<Feed>();
    }

    /**
     * Get all missions that contains this content UID
     * @param uid Content UID
     * @return List of feeds containing this UID
     */
    public List<Feed> findFeedsByContent(final String uid) {
        List<Feed> feeds = new ArrayList<>();
        if (FileSystemUtils.isEmpty(uid))
            return feeds;

        // We only care about missions stored locally
        for (Feed feed : getLocalFeeds()) {
            if (feed.hasContent(uid))
                feeds.add(feed);
        }
        return feeds;
    }

    /**
     * Check if a content UID is part of any local feeds
     * @param uid Content UID
     * @return The feed that has this UID or null if not found
     */
    public Feed findFeedByContent(String uid) {
        List<Feed> feeds = findFeedsByContent(uid);
        return !feeds.isEmpty() ? feeds.get(0) : null;
    }

    /**
     * Get the number of feeds
     * @return Feed count
     */
    public int getFeedCount() {
        synchronized (_feeds) {
            return _feeds.size();
        }
    }

    /**
     * Get the total number of unread items across all feeds
     * @return Unread total
     */
    public int getTotalUnreadCount() {
        int total = 0;
        for (Feed f : getFeeds())
            total += f.getUnreadCount();
        return total;
    }

    /**
     * Listener for when the registered feeds list changes
     * What makes this different from the several other listeners
     * is that it's tied directly to the modification of getFeeds()
     */
    public interface Listener {
        void onFeedsAdded(List<Feed> feeds);
        void onFeedsUpdated(List<Feed> feeds);
        void onFeedsRemoved(List<Feed> feeds);
    }

    public synchronized void addListener(Listener l) {
        _listeners.add(l);
    }

    public synchronized void removeListener(Listener l) {
        _listeners.remove(l);
    }

    private synchronized List<Listener> getListeners() {
        return new ArrayList<>(_listeners);
    }

    private void onFeedsAdded(List<Feed> feeds) {
        // Always read from a copy of the listener set to avoid potential
        // concurrent modification issues
        for (Listener l : getListeners())
            l.onFeedsAdded(feeds);
    }

    public void onFeedsUpdated(List<Feed> feeds) {
        for (Listener l : getListeners())
            l.onFeedsUpdated(feeds);
    }

    private void onFeedsRemoved(List<Feed> feeds) {
        for (Listener l : getListeners())
            l.onFeedsRemoved(feeds);
    }
}
