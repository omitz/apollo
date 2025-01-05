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

package com.atakmap.android.missionapi.net.http.get;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.missionapi.data.FileChangeWatcher;
import com.atakmap.android.missionapi.model.json.FeedFileDetails;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;

import java.io.File;
import java.io.IOException;

/**
 * Created by byoung on 4/12/2016.
 */
public class GetFeedAttachmentOperation extends GetFeedFileOperation {

    private static final String TAG = "GetFeedAttachmentOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException {

        // Get request data
        GetFeedAttachmentRequest req = (GetFeedAttachmentRequest) r;

        try {
            FeedFileDetails details = new FeedFileDetails(new JSONData(get(r),
                    true));
            Log.d(TAG, "Parsed resource, now downloading attachment");

            //then get file, place in feed folder
            String filepath = getFile(context,
                    req.getDelay(),
                    details, req.getFeedName(),
                    req.getFeedUID(),
                    req.getServerURL(), req.getNotificationID());
            File downloadedFile = new File(filepath);
            if (!FileSystemUtils.isFile(downloadedFile)) {
                throw new IOException("Failed to create file: " + filepath);
            }

            //place in attachment folder for referenced marker or user log
            File destFolder = new File(req.getDestinationDir());
            if (!destFolder.exists())
                destFolder.mkdirs();
            //TODO check before overwriting? Should not have multiple attachments of same name for a given UID
            //now move file out of incoming, rename prior to prcoessing
            File dest = new File(destFolder, details.getName());

            if (!FileChangeWatcher.getInstance().renameTo(downloadedFile, dest)) {
                FileSystemUtils.deleteFile(downloadedFile);
                throw new ConnectionException("Failed to rename file");
            }

            b.putString(RestManager.RESPONSE_JSON, details.toJSON(true)
                    .toString());
            b.putString("local_path", dest.getAbsolutePath());
        } catch (TakHttpException e) {
            Log.e(TAG, "Failed to download feed file attachment", e);
            throw new ConnectionException(e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            Log.e(TAG, "Failed to download feed file attachment", e);
            throw new ConnectionException(e.getMessage(),
                    NetworkOperation.STATUSCODE_UNKNOWN);
        }
    }
}
