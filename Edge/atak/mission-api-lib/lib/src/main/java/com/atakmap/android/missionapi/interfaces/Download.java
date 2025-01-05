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

package com.atakmap.android.missionapi.interfaces;

import com.atakmap.android.hierarchy.action.Action;

import java.util.List;

/**
 * Interface for downloadable items
 */
public interface Download extends Action {

    /**
     * Get the number of downloads available
     * @return Number of available downloads
     */
    int getDownloadCount();

    /**
     * Retrieve all downloadable items
     * @return List of downloadable items
     */
    List<Download> getDownloads();

    /**
     * Begin download of content
     * @return True if download started, false if error
     */
    boolean download();
}
