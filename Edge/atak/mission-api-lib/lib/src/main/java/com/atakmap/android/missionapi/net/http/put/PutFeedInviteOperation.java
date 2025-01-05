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

package com.atakmap.android.missionapi.net.http.put;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.missionapi.data.TAKServerVersion;
import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.put.PutFeedInviteRequest.Invitation;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;

import java.util.List;

/**
 * Operation to HTTP PUT a feed subscription (or DELETE it)
 *
 */
public class PutFeedInviteOperation extends AbstractOperation {
    
    private static final String TAG = "PutFeedInviteOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException,
            DataException {

        PutFeedInviteRequest req = (PutFeedInviteRequest) r;
        if (req.checkSupport(TAKServerVersion.SUPPORT_BULK_INVITE)) {
            // Only one request necessary
            serverRequest(r, req.getOperationType(), b);
        } else {
            // Need to do several requests - much more inefficient
            List<Invitation> invitations = req.getInvitations();
            for (Invitation inv : invitations)
                serverRequest(r, req.getOperationType(), b, inv.uid,
                        inv.callsign, String.valueOf(inv.role));
        }
    }
}
