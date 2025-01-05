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

import com.atakmap.android.missionapi.R;

public enum AvailabilityState {

    NEW(R.string.new_content, R.color.new_content_text, R.color.new_content_bg),
    AVAILABLE(R.string.available, R.color.error_text, R.color.error_bg),
    MISSING(R.string.missing, R.color.missing_text, R.color.missing_bg),
    DESYNCED(R.string.desynced, R.color.desynced_text, R.color.desynced_bg),
    OFFLINE(R.string.offline, R.color.desynced_text, R.color.desynced_bg),
    OK(0, 0, 0);

    public final int stringId, textColorId, bgColorId;

    AvailabilityState(int stringId, int textColorId, int bgColorId) {
        this.stringId = stringId;
        this.textColorId = textColorId;
        this.bgColorId = bgColorId;
    }
}
