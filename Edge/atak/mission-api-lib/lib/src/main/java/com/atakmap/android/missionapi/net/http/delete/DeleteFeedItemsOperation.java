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

package com.atakmap.android.missionapi.net.http.delete;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.OpType;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;

/**
 * Operation to HTTP DELETE a CoT UID
 *
 */
public class DeleteFeedItemsOperation extends AbstractOperation {

    private static final String TAG = "DeleteFeedItemsOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException,
            DataException {

        // Get request data
        DeleteFeedItemsRequest req = (DeleteFeedItemsRequest) r;

        int itemCount = 0, currentProgress = 0;

        for (String uid : req.getUIDs()) {
            currentProgress = (int) Math.round((double) itemCount
                    / (double) req.getUIDs().size() * 100) + 1;
            itemCount++;

            if (FileSystemUtils.isEmpty(uid)) {
                throw new DataException("Empty UID");
            }

            String message = String.format(LocaleUtil.getCurrent(),
                    "Deleting %d of %d items (%d%%) - %s", itemCount,
                    req.getUIDs().size(),
                    currentProgress, req.getFeedName());
            Log.d(TAG, message + ". " + req.toString());

            serverRequest(req, OpType.DELETE, uid);
        }
    }
}
