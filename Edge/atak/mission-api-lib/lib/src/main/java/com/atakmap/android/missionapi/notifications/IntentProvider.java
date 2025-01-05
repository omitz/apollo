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

package com.atakmap.android.missionapi.notifications;

import android.content.Intent;

/**
 * Provides intents given some data
 */
public interface IntentProvider {

    /**
     * Create an intent given some arguments
     * @param args Arguments
     * @return New intent or null if N/A
     */
    Intent createIntent(Object... args);
}
