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

package com.atakmap.android.missionapi.model.json.user;

/**
 * Individual permissions of a user
 */
public class UserPermission {

    // Flags
    public static final int READ = 1,
            WRITE = 1 << 1,
            DELETE = 1 << 2,
            SET_ROLE = 1 << 3,
            SET_PASSWORD = 1 << 4,
            UPDATE_GROUPS = 1 << 5,
            ALL = READ | WRITE | DELETE | SET_ROLE | SET_PASSWORD | UPDATE_GROUPS;
}
