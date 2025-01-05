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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Changes mapped by change UID and sorted by timestamp
 * Note: Not using TreeMap here because it keys by the compare result,
 * rather than the actual key. Not confusing or misleading at all...
 */
public class FeedChangeMap {

    public static final Comparator<FeedChange> TIME_SORT
            = new Comparator<FeedChange>() {
        @Override
        public int compare(FeedChange lhs, FeedChange rhs) {
            return Long.compare(rhs.time, lhs.time);
        }
    };

    // Map creator/type/timestamp/UID to change
    private final Map<String, FeedChange> _backingMap = new HashMap<>();

    // Sorted list of changes updated whenever the backing map changes
    private final List<FeedChange> _sorted = new ArrayList<>();

    public FeedChangeMap() {
    }

    public FeedChangeMap(List<FeedChange> changes) {
        this();
        addAll(changes);
    }

    public FeedChange add(FeedChange change) {
        if (change == null)
            return null;
        FeedChange ret = _backingMap.put(change.getUID(), change);
        sort();
        return ret == null ? change : null;
    }

    public List<FeedChange> addAll(Collection<FeedChange> changes) {
        List<FeedChange> ret = new ArrayList<>();
        for (FeedChange ch : changes) {
            FeedChange oldVal = _backingMap.put(ch.getUID(), ch);
            if (oldVal == null)
                ret.add(ch);
            else
                ch.setUnread(oldVal.isUnread());
        }
        sort();
        return ret;
    }

    public FeedChange get(FeedChange change) {
        return _backingMap.get(change.getUID());
    }

    public List<FeedChange> getSortedList() {
        return new ArrayList<>(_sorted);
    }

    public FeedChange getFirst() {
        return !isEmpty() ? _sorted.get(0) : null;
    }

    public FeedChange getLast() {
        return !isEmpty() ? _sorted.get(_sorted.size() - 1) : null;
    }

    public FeedChange remove(FeedChange change) {
        FeedChange ch = _backingMap.remove(change.getUID());
        sort();
        return ch;
    }

    public void clear() {
        _backingMap.clear();
        _sorted.clear();
    }

    public boolean isEmpty() {
        return _backingMap.isEmpty();
    }

    private void sort() {
        _sorted.clear();
        _sorted.addAll(_backingMap.values());
        Collections.sort(_sorted, TIME_SORT);
    }
}
