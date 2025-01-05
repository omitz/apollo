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

package com.atakmap.android.missionapi;

import android.content.Context;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.data.FileChangeWatcher;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.notifications.NotificationBuilder;
import com.atakmap.android.missionapi.util.IconUtils;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.log.Log;

/**
 * The main class used to initialize this library
 */
public class MissionAPI {

    private static final String TAG = "MissionAPI";

    /**
     * Initialize the API
     * @param pluginCtx Plugin context (or main context for whichever app dependency)
     */
    public static void init(MapView mapView, Context pluginCtx) {
        try {
            // Initialize utilities
            StringUtils.init(pluginCtx);
            IconUtils.init(pluginCtx);
            NotificationBuilder.init(pluginCtx);
            new FileChangeWatcher(mapView);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MissionAPI library", e);
        }
    }

    /**
     * Dispose library components
     */
    public static void dispose() {
        RestManager.getInstance().dispose();
    }
}
