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

import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.filesystem.HashingUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Watch feed files for modifications and removal
 */
public class FileChangeWatcher extends Thread {

    private static final String TAG = "FileChangeWatcher";
    private static final int SCAN_INTERVAL = 1000;

    private static class HashModTime {
        long modTime;
        String hash;

        HashModTime(long modTime, String hash) {
            this.modTime = modTime;
            this.hash = hash;
        }
    }

    public interface Listener {

        /**
         * A feed file has been modified
         * @param feed Feed
         * @param oldFile Old file
         * @param newFile New file
         */
        void onFileModified(Feed feed, FeedFile oldFile, FeedFile newFile);

        /**
         * A feed file has been removed
         * @param feed Feed
         * @param file File
         */
        void onFileRemoved(Feed feed, FeedFile file);
    }

    private static FileChangeWatcher _instance;

    public static FileChangeWatcher getInstance() {
        return _instance;
    }

    private final MapView _mapView;
    private final Map<File, HashModTime> _modTime = new HashMap<>();
    private final Set<File> _ignoreFiles = new HashSet<>();
    private final Set<File> _resetFiles = new HashSet<>();
    private final Set<Listener> _listeners = new HashSet<>();
    private boolean _started, _clearCache;

    public FileChangeWatcher(MapView mapView) {
        super(TAG);
        _mapView = mapView;
        if (_instance == null)
            _instance = this;
    }

    @Override
    public void start() {
        if (!_started) {
            _started = true;
            super.start();
        }
    }

    public void dispose() {
        // Thread performs the disposal on its own
        _started = false;
    }

    public synchronized void clearCache() {
        _clearCache = true;
    }

    @Override
    public void run() {
        while (_started) {
            scan();
            try {
                Thread.sleep(SCAN_INTERVAL);
            } catch (Exception ignore) {
            }
        }
        // Thread has been disposed
        _modTime.clear();
    }

    /**
     * Mark this file as ignored - the scan should not check this file
     * @param f File to ignore
     */
    public synchronized void ignore(File f) {
        if (f == null)
            return;
        _ignoreFiles.add(f);
        _resetFiles.add(f);
    }

    /**
     * Remove file from ignore list
     * @param f File to stop ignoring
     */
    public synchronized void unignore(File f) {
        if (f == null)
            return;
        _ignoreFiles.remove(f);
    }

    /**
     * Rename a file while ignoring any changes to both
     * @param src Source file
     * @param dst Destination file
     * @return True if renamed
     */
    public boolean renameTo(File src, File dst) {
        ignore(src);
        ignore(dst);
        try {
            return FileSystemUtils.renameTo(src, dst);
        } finally {
            unignore(src);
            unignore(dst);
        }
    }

    /**
     * Scan for files in feeds and check if they've been changed or removed
     * since the last time this scan run
     */
    private void scan() {

        // Reset cache for certain files
        synchronized (this) {
            if (_clearCache) {
                _modTime.clear();
                _ignoreFiles.clear();
                _clearCache = false;
            } else {
                for (File f : _resetFiles)
                    _modTime.remove(f);
            }
            _resetFiles.clear();
        }

        // Check if any feed files have changed
        Set<File> feedFiles = new HashSet<>();
        List<Feed> feeds = FeedManager.getInstance().getSubscribedFeeds();
        for (Feed feed : feeds) {
            List<FeedFile> files = feed.getFiles();
            for (FeedFile file : files) {
                checkFileChanged(feed, file);

                File f = file.getLocalFile();
                if (f != null)
                    feedFiles.add(f);
            }
        }

        // Remove files no longer valid
        Set<File> fileSet = new HashSet<>(_modTime.keySet());
        fileSet.removeAll(feedFiles);
        for (File f : fileSet)
            _modTime.remove(f);
    }

    /**
     * Check if a file has changed by comparing its last modified time and hash
     * @param feed Feed
     * @param file File
     */
    private void checkFileChanged(final Feed feed, final FeedFile file) {
        File f = file.getLocalFile();
        if (f == null || _ignoreFiles.contains(f))
            return;

        long modTime = f.exists() ? f.lastModified() : 0L;
        HashModTime existing = _modTime.get(f);
        if (existing == null) {
            if (f.exists())
                _modTime.put(f, new HashModTime(modTime, file.getHash()));
            return;
        }

        if (existing.modTime == modTime)
            return;

        final boolean removed = existing.modTime != 0L && modTime == 0L;
        existing.modTime = modTime;

        String hash = removed ? null : HashingUtils.sha256sum(f);

        if (FileSystemUtils.isEquals(existing.hash, hash))
            return;
        existing.hash = hash;

        final FeedFile newFile = removed ? null : Feed.createFile(feed,
                file.getDetails());
        if (newFile != null) {
            newFile.hash = hash;
            newFile.size = f.length();
            newFile.setTime(new CoordinatedTime().getMilliseconds());
            newFile.localPath = f.getAbsolutePath();
            newFile.setAttachmentUID(file.getAttachmentUID());
        } else
            _modTime.remove(f);

        _mapView.post(new Runnable() {
            @Override
            public void run() {
                for (Listener l : getListeners()) {
                    if (removed)
                        l.onFileRemoved(feed, file);
                    else
                        l.onFileModified(feed, file, newFile);
                }
            }
        });
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
}
