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

package com.atakmap.android.missionapi.net.http;

import com.atakmap.android.http.rest.DownloadProgressTracker;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.foxykeep.datadroid.requestmanager.Request;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks outstanding REST operations. Used to prevent duplicate downloads or publishes of the
 * same content
 *
 * Does not handle time-outs, as HTTP connections provide that along with listener/error callbacks
 * TODO need to handle error conditions that leave a UID permanently pending e.g. cant download
 * a file w/out restarting ATAK?
 */
public class PendingRestOperation {
    public enum OpType {
        UPLOAD,
        DOWNLOAD
    }

    public interface Listener {
        void onStart(PendingRestOperation op);
        void onProgress(PendingRestOperation task, DownloadProgressTracker tr);
        void onComplete(PendingRestOperation op);
    }

    public final OpType opType;
    public final String title;
    public final String uid;
    public final String missionUid;
    public final Request request;

    private int _progress = 0;
    private final Set<Listener> _listeners = new HashSet<>();

    PendingRestOperation(String m, OpType ty, String t, String u,
                         Request r) {
        opType = ty;
        missionUid = m;
        title = t;
        uid = u;
        request = r;
    }

    public String getKey() {
        return generateUID(missionUid, opType.toString(), uid);
    }

    public void onStart() {
        FeedUtils.post(new Runnable() {
            @Override
            public void run() {
                for (Listener l : getListeners())
                    l.onStart(PendingRestOperation.this);
            }
        });
    }

    public void onProgress(final DownloadProgressTracker tracker) {
        FeedUtils.post(new Runnable() {
            @Override
            public void run() {
                _progress = tracker.getCurrentProgress();
                for (Listener l : getListeners())
                    l.onProgress(PendingRestOperation.this, tracker);
            }
        });
    }

    public void onComplete() {
        FeedUtils.post(new Runnable() {
            @Override
            public void run() {
                for (Listener l : getListeners())
                    l.onComplete(PendingRestOperation.this);
            }
        });
    }

    public int getProgress() {
        return _progress;
    }

    public void addListener(Listener l) {
        synchronized (_listeners) {
            _listeners.add(l);
        }
    }

    public List<Listener> getListeners() {
        synchronized (_listeners) {
            return new ArrayList<>(_listeners);
        }
    }

    public static String generateUID(String... strings) {
        if (FileSystemUtils.isEmpty(strings))
            return UUID.randomUUID().toString();

        StringBuilder key = new StringBuilder();
        for (String s : strings)
            key.append(s).append(".");
        return key.toString();
    }
}