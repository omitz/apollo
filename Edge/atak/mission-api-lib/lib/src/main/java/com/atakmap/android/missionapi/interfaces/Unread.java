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

/**
 * Interface for items that have a read/unread state
 */
public interface Unread extends Action {

    /**
     * Get the number of unread items
     * @return Number of unread items
     */
    int getUnreadCount();
}
