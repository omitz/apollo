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

package com.atakmap.android.missionapi.model.xml;


import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedChange;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Legacy XML-based mission storage
 * Should ONLY be used for converting missions from XML to JSON
 */
public class XMLMissionDatabase {

    private static final String TAG = "XMLMissionDatabase";

    // Mission manifests table
    private static final String TABLE_MISSIONS = "missions";

    // Primary key
    private static final String COLUMN_ID = "_id";

    // Mission UID
    private static final String COLUMN_UID = "_uid";

    // XML encoded mission manifest
    private static final String COLUMN_XML = "_xml";

    // JSON encoded change log
    private static final String COLUMN_CHANGELOG = "_clog";

    private DatabaseIface _db;

    public XMLMissionDatabase(File dbFile) {
        _db = Databases.openOrCreateDatabase(dbFile.getAbsolutePath());
    }

    public void dispose() {
        if (_db != null)
            _db.close();
        _db = null;
    }

    /**
     * Get all missions in the database
     * @return List of missions
     */
    public List<Feed> getFeeds() {
        CursorIface cursor = null;

        List<Feed> ret = new ArrayList<>();
        try {
            cursor = _db.query("SELECT * FROM " + TABLE_MISSIONS, null);
            while (cursor.moveToNext()) {
                XMLMissionManifest mission = getMission(cursor);
                if (mission == null)
                    continue;

                Feed feed = Feed.create(mission);
                List<FeedChange> changes = getChanges(feed, cursor);
                feed.changes.addChanges(changes, false, false);
                ret.add(feed);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load XML missions", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return ret;
    }

    private XMLMissionManifest getMission(CursorIface cursor) {
        try {
            return XMLMissionManifest.fromXml(cursor.getString(
                    cursor.getColumnIndex(COLUMN_XML)));
        } catch (Exception e) {
            Log.e(TAG, "Failed to read mission manifest cursor", e);
        }
        return null;
    }

    private List<FeedChange> getChanges(Feed feed, CursorIface cursor) {
        List<FeedChange> ret = new ArrayList<>();
        try {
            String cLog = cursor.getString(cursor.getColumnIndex(
                    COLUMN_CHANGELOG));
            if (FileSystemUtils.isEmpty(cLog))
                return ret;

            JSONData data = new JSONData(cLog, false);
            if (!FileSystemUtils.isEquals(data.get("type"),
                    "MissionChange"))
                return ret;

            return data.getList("data", feed, FeedChange.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to read mission changes cursor", e);
        }
        return ret;
    }
}
