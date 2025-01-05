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

package com.atakmap.android.missionapi.net.http.listener;

import android.os.Bundle;

import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.foxykeep.datadroid.requestmanager.Request;

/**
 * Single-method request listener that uses {@link AbstractRequest}
 */
public abstract class AbstractRequestListener extends SimpleRequestListener {

    public abstract void onRequestResult(AbstractRequest request, Bundle resultData, int responseCode);

    public void onRequestResult(Request request, Bundle resultData, int responseCode) {
        AbstractRequest r = AbstractRequest.fromRequest(request);
        if (r != null)
            onRequestResult(r, resultData, responseCode);
    }
}
