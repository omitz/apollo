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

import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.requestmanager.RequestManager.ConnectionError;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestListener;

import org.apache.http.HttpStatus;

/**
 * Single-method request listener
 */
public abstract class SimpleRequestListener implements RequestListener {

    public abstract void onRequestResult(Request request, Bundle resultData, int responseCode);

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {
        onRequestResult(request, resultData, HttpStatus.SC_OK);
    }

    @Override
    public void onRequestConnectionError(Request request, ConnectionError ce) {
        onRequestResult(request, null, ce.getStatusCode());
    }

    @Override
    public void onRequestDataError(Request request) {
        onRequestResult(request, null, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Override
    public void onRequestCustomError(Request request, Bundle bundle) {
        onRequestResult(request, bundle, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
}
