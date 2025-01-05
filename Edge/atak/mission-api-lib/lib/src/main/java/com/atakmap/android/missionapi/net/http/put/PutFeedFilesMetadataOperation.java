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

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.notifications.NotificationBuilder;
import com.atakmap.android.missionapi.notifications.RecentActivity;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.comms.http.TakHttpClient;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic operation to associate hashes with a feed
 * Assumes hashes are already on server. Does not update local feed manifest
 *
 * TODO: This is near identical to {@link PutFeedFilesOperation}
 */
public class PutFeedFilesMetadataOperation extends AbstractOperation {

    private static final String TAG = "PutFeedFilesMetadataOperation";

    @Override
    public void execute(Context context, AbstractRequest request, Bundle b)
            throws ConnectionException {

        // Get request data
        PutFeedFilesMetadataRequest putReq =
                (PutFeedFilesMetadataRequest) request;

        int fileCount = 0, currentProgress = 0;

        // setup progress notifications
        List<FeedFile> files = putReq.getContents();
        NotificationManager notifyManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationBuilder builder = new NotificationBuilder();
        String message = "Publishing " + files.size() + " files to "
                + putReq.getFeedName() + "...";
        builder.setContentTitle(message)
                .setContentText(message)
                .setSmallIcon(
                        NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID());

        ArrayList<String> responses = new ArrayList<>();
        ArrayList<String> hashes = new ArrayList<>();
        for (FeedFile file : files) {
            currentProgress = (int) Math.round((double) fileCount
                    / (double) files.size() * 100) + 1;
            fileCount++;

            message = String.format(LocaleUtil.getCurrent(),
                    "Publishing %d of %d files (%d%%) - %s - %s", fileCount,
                    files.size(),
                    currentProgress, putReq.getFeedName(),
                    file.getName());

            if (files.size() > 1
                    && putReq.getNotificationID() > 0) {
                builder.setProgress(currentProgress, 100);
                builder.setContentText(message);
                notifyManager.notify(putReq.getNotificationID(),
                        builder.build());
            }
            Log.d(TAG, message + ". " + putReq.toString());
            Pair<String, String> p = publish(request, file);
            if (p == null)
                continue;
            hashes.add(p.first);
            responses.add(p.second);
        }

        message = "Published " + files.size()
                + " files to " + putReq.getFeedName();
        Log.d(TAG, message + ". " + putReq.toString());
        //display multi-file notification if more than 1 file, and a notify ID is provided
        if (files.size() > 1
                && putReq.getNotificationID() > 0) {
            notifyManager.cancel(putReq.getNotificationID());
            RecentActivity.notify(putReq.getFeedUID(),
                    message, message);
        }

        b.putStringArrayList(RestManager.RESPONSE_JSON, responses);
        b.putStringArrayList(RestManager.RESPONSE_HASHES, hashes);
    }

    /**
     * If hash is on server, associate with feed, otherwise error
     */
    private static Pair<String, String> publish(AbstractRequest req,
            FeedFile file) throws ConnectionException {

        TakHttpClient client = null;
        try {
            client = TakHttpClient.GetHttpClient(req.getServerURL());

            //first verify hash already exists on server...
            String hashUrl = client.getUrl("sync/content");
            Uri.Builder builder = Uri.parse(hashUrl).buildUpon();
            builder.appendQueryParameter("hash", file.getHash());
            hashUrl = builder.build().toString();
            hashUrl = FileSystemUtils.sanitizeURL(hashUrl);

            if (!client.head(hashUrl)) {
                Log.w(TAG, "Hash not on server: " + hashUrl);
                return null;
            }

            Log.d(TAG, "Content exists on server: " + file);

            //now associate file/hash with this feed
            String responseBody = PutFeedFilesOperation
                    .associateWithServerFeed(req,
                            Collections.singletonList(file.getHash()),
                            client);
            return new Pair<>(file.getHash(), responseBody);
        } catch (TakHttpException e) {
            Log.e(TAG, "Failed to associate: " + file.getHash(), e);
            throw new ConnectionException(e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            Log.e(TAG, "Failed to associate: " + file.getHash(), e);
            throw new ConnectionException(e.getMessage(),
                    NetworkOperation.STATUSCODE_UNKNOWN);
        } finally {
            try {
                if (client != null)
                    client.shutdown();
            } catch (Exception ignore) {
            }
        }
    }
}
