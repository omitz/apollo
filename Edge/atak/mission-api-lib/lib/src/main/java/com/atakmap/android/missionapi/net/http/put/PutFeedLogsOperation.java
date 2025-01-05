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
import android.util.Pair;

import com.atakmap.android.missionapi.data.FeedManager;
import com.atakmap.android.missionapi.model.json.AttachmentList;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.android.missionapi.model.json.FeedFileDetails;
import com.atakmap.android.missionapi.model.json.FeedLog;
import com.atakmap.android.missionapi.model.json.HashResourceFile;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpClient;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.comms.http.TakHttpResponse;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic operation to associate log messages with a feed
 * Used to add or update logs
 *
 */
public class PutFeedLogsOperation extends AbstractOperation {

    private static final String TAG = "PutFeedLogsOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException,
            DataException {

        // Get request data
        PutFeedLogsRequest req = (PutFeedLogsRequest) r;

        int logCount = 0, currentProgress = 0;

        // setup progress notifications
        /*NotificationManager notifyManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationBuilder builder = new NotificationBuilder(context);
        builder.setContentTitle("Publishing logs to "
                        + req.getFeedName()+ "...")
                .setContentText("Publishing logs to "
                                + req.getFeedName() + "...")
                .setSmallIcon(com.atakmap.android.util.NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID()
        );*/

        List<AttachmentList> atts = new ArrayList<>();
        for (FeedLog curLog : req.getLogs()) {
            currentProgress = (int) Math.round((double) logCount
                    / (double) req.getLogs().size() * 100) + 1;
            logCount++;

            if (curLog == null || !curLog.isValid()) {
                throw new DataException("Invalid log");
            }

            String message = String.format(LocaleUtil.getCurrent(),
                    "Publishing %d of %d logs (%d%%) - %s", logCount,
                    req.getLogs().size(),
                    currentProgress, req.getFeedName());
            /*builder.setProgress(100, currentProgress, false);
            builder.setContentText(message);
            notifyManager.notify(req.getNotificationId(),
                    builder.getNotification());*/
            Log.d(TAG, message + ". " + req.toString());

            //publish this log
            AttachmentList att = publish(context, req, curLog);
            atts.add(att);

            try {
                //now get manifest, and update list of logs, and their IDs
                replaceLog(context, req.getFeedUID(), curLog, att);

            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize JSON response", e);
            }
        }


        try {
            String attliststring = AttachmentList.toJSONList(atts).toString();
            //Log.d(TAG, "Response Attachment list: " + attliststring);
            b.putString(RestManager.RESPONSE_JSON, attliststring);
        } catch (JSONException e) {
            throw new DataException(e);
        }
    }

    private static void replaceLog(Context context, String feedUID,
            FeedLog localLog,
            AttachmentList att) throws JSONException {

        //lookup feed
        Feed feed = FeedManager.getInstance().getFeed(feedUID);
        if (feed == null || !feed.isValid()) {
            Log.w(TAG, "Feed not found, cannot update: " + feedUID);
            return;
        }

        JSONData res = new JSONData(att.getPrimaryResponse(), true);
        FeedLog serverLog = Feed.createLog(feed, res.getChild("data"));
        if (!serverLog.isValid()) {
            Log.w(TAG, "No server log found, cannot update: " + feedUID);
            return;
        }

        //find log contents so we can remove all existing logs
        if (feed.removeContent(localLog) == null) {
            Log.w(TAG, "Feed log not found, cannot remove old log: "
                    + localLog.toString());
            return;
        } else {
            // If we successfully removed the local log entry, we need to update the attachments with the UID of the server log entry
            List<FeedFile> attachments = feed.getAttachedFiles(localLog.getUID());
            for (FeedFile attachment : attachments) {
                Log.d(TAG, "Changing attachment ID from " + localLog.getUID()
                        + " to " + serverLog.getUID());
                attachment.setAttachmentUID(serverLog.getUID());
                String localPath = attachment.getLocalPath();
                if (localPath != null && localPath.contains(localLog.getUID())) {
                    attachment.setLocalPath(localPath.replace(localLog.getUID(),
                            serverLog.getUID()));
                }
                String oldPath = attachment.getLocalPath().substring(0,
                        attachment.getLocalPath().lastIndexOf(File.separator));
                Log.d(TAG, "The old path is " + oldPath);
                if (oldPath.contains(localLog.getUID())) {
                    String newPath = oldPath.replace(localLog.getUID(),
                            serverLog.getUID());
                    Log.d(TAG, "Renaming attachments folder from " + oldPath
                            + " to " + newPath);
                    FileSystemUtils.renameTo(new File(oldPath),
                            new File(newPath));
                }
            }
        }

        feed.addContent(serverLog);

        //also need to set resource metadata and "published" flag on local manifest for all attachments we just pushed
        if (att.hasResponses()) {
            for (String attachmentResponse : att.getResponses()) {
                FeedFileDetails details = new FeedFileDetails(
                        new JSONData(attachmentResponse, true));

                FeedFile attachment = feed.getContent(details.getHash());
                if (attachment != null) {
                    Log.d(TAG,
                            "Setting resource for: " + attachment.toString());
                    attachment.setDetails(details);
                } else {
                    //TODO these should have been added by UI code prior to publishing to server
                    //We could re-add here if not found...
                    Log.w(TAG,
                            "No existing file found, cannot update attachment resource");
                }
            }
        }

        //finally update DB
        Log.d(TAG, "Replacing " + localLog.toString() + " log with "
                + serverLog.toString() + " for feed: " + feed);
        feed.persist();
    }

    private static AttachmentList publish(Context context,
                                          PutFeedLogsRequest req, FeedLog log)
            throws ConnectionException, DataException {

        Feed feed = FeedManager.getInstance().getFeed(req.getFeedUID());
        if (feed == null || !feed.isValid()) {
            Log.w(TAG, "Feed not found, cannot publish logs: " + feed);
            throw new DataException("Unable to find feed");
        }

        TakHttpClient client = null;
        try {
            client = TakHttpClient.GetHttpClient(req.getServerURL());

            //see if we should upload attachments
            List<String> processedHashes = new ArrayList<String>();
            List<String> attachmentResponses = new ArrayList<String>();
            List<String> localPaths = new ArrayList<String>();
            List<HashResourceFile> files = req.getFiles();
            if (!FileSystemUtils.isEmpty(files)) {
                Log.d(TAG, "Processing file count: " + files.size());
                for (HashResourceFile file : files) {
                    Log.d(TAG, "Processing file: " + file.toString());
                    //skip child directories
                    if (!file.isValid() || !FileSystemUtils
                            .isFile(file.getFile().getFilePath())) {
                        Log.w(TAG, "Skip invalid file");
                        continue;
                    }

                    //see if file already in feed
                    String mimeType = file.getFile().getMimeType();
                    String contentType = file.getFile().getContentType();
                    FeedFile existing = feed.getContent(file.getHash());
                    if (existing != null && existing.isValid()) {
                        //TODO if already in manifest, can we just skip the publish?
                        //use existing MIME/Content Type
                        Log.d(TAG, "Processing existing file: "
                                + existing.toString());
                        if (existing.mimeType != null)
                            mimeType = existing.mimeType;
                        String cType = existing.getContentType();
                        if (cType != null)
                            contentType = cType;
                    } else {
                        Log.d(TAG, "Processing new file: " + file.toString());
                    }

                    //publish file and add hash to list we are tracking
                    Pair<String, String> p = PutFeedFilesOperation.publish(
                            context, req,
                            new File(file.getFile().getFilePath()),
                            mimeType, contentType);
                    if (!FileSystemUtils.isEmpty(p.first)) {
                        Log.d(TAG, "Including file hash: " + p.first
                                + ", for log: " + log.getUID());
                        processedHashes.add(p.first);
                        attachmentResponses.add(p.second);
                        localPaths.add(file.getFile().getFilePath());
                    } else {
                        Log.d(TAG,
                                "Unable to publish file hash: "
                                        + file.getFile().getFilePath()
                                        + ", for log: " + log.getUID());
                    }
                } //end attachment loop
            } else {
                Log.d(TAG, "Not processing files");
            }

            //see if this has a locally generated UID, or a server UID has been assigned
            boolean bUpdate = !FileSystemUtils.isEmpty(log.serverTime);

            //TODO fault tolerance... how to handle upload/post error?
            String requestUrl = client.getUrl("api/missions/logs/entries");
            requestUrl = FileSystemUtils.sanitizeURL(requestUrl);

            HttpEntityEnclosingRequestBase httpRequest = bUpdate
                    ? new HttpPut(requestUrl)
                    : new HttpPost(requestUrl);
            httpRequest.addHeader("Content-Type", HttpUtil.MIME_JSON);
            httpRequest.addHeader("Accept-Encoding", HttpUtil.GZIP);

            // Add additional request headers
            addHeaders(httpRequest, req);

            if (!FileSystemUtils.isEmpty(processedHashes)) {
                Log.d(TAG,
                        "Processed hashes with log: " + processedHashes.size());
            }

            //TODO pre-calculate hashes and publish log message prior to posting attachment files (similar to how we handle CoT publish)?
            //so other users can get the log message sooner
            JSONData logData = log.toJSON(true);
            if (!bUpdate)
                logData.remove("id");
            logData.remove("servertime");
            logData.set("missionNames", Collections.singletonList(feed.name));
            String requestBody = logData.toString();
            StringEntity se = new StringEntity(requestBody);
            se.setContentType(HttpUtil.MIME_JSON);
            se.setContentEncoding(FileSystemUtils.UTF8_CHARSET.name());
            httpRequest.setEntity(se);

            Log.d(TAG, httpRequest.getClass() + ", associating "
                    + log.toString() + " with feed "
                    + feed.getName() + ", "
                    + httpRequest.getRequestLine() + ". " + requestBody);

            // post (or put) file
            TakHttpResponse response = client.execute(httpRequest);
            String primaryResponseBody = response.getStringEntity(
                    RestManager.MATCHER_LOGENTRY);
            response.verify(HttpStatus.SC_CREATED);

            //bundle it all up for return
            return new AttachmentList(log.getUID(), primaryResponseBody,
                    localPaths, processedHashes, attachmentResponses);
        } catch (TakHttpException e) {
            Log.e(TAG, "Failed to put: " + feed, e);
            throw new ConnectionException(e.getMessage(), e.getStatusCode());
        } catch (ConnectionException e) {
            Log.e(TAG, "Failed to put: " + feed, e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Failed to put: " + feed, e);
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
