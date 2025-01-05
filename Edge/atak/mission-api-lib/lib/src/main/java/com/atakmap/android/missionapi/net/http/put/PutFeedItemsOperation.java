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

import com.atakmap.android.missionapi.model.json.AttachmentList;
import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpClient;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.comms.http.TakHttpResponse;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic operation to PUT files and then associate hashes with a feed
 *  Assumes item CoT was already sent to the server, so the UID is stored on the server
 *  TODO if streaming connection currently down, then that would have failed, but this HTTP
 *  is out of band wrt to streaming connection
 *
 */
public class PutFeedItemsOperation extends AbstractOperation {

    private static final String TAG = "PutFeedItemsOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException,
            DataException {

        // Get request data
        PutFeedItemsRequest req = (PutFeedItemsRequest) r;

        int itemCount = 0, currentProgress = 0;

        // setup progress notifications
        /*NotificationManager notifyManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationBuilder builder = new NotificationBuilder(context);
        builder.setContentTitle("Publishing items to "
                        + req.getFeedName()+ "...")
                .setContentText("Publishing items to "
                                + req.getFeedName() + "...")
                .setSmallIcon(com.atakmap.android.util.NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID()
        );*/

        List<AttachmentList> atts = new ArrayList<AttachmentList>();
        if (req.hasAttachments()) {
            for (AttachmentList al : req.getAttachments()) {
                currentProgress = (int) Math.round((double) itemCount
                        / (double) req.getAttachments().size() * 100)
                        + 1;
                itemCount++;

                if (al == null || !al.isValid()) {
                    throw new DataException("Empty UID");
                }

                String curName = "item";
                MapItem item = FeedUtils.findMapItem(al.getUid());
                if (item != null) {
                    curName = item.getMetaString("callsign", curName);
                }

                String message = String.format(LocaleUtil.getCurrent(),
                        "Publishing %d of %d items (%d%%) - %s - %s", itemCount,
                        req.getAttachments().size(),
                        currentProgress, req.getFeedName(), curName);
                /*builder.setProgress(100, currentProgress, false);
                builder.setContentText(message);
                notifyManager.notify(req.getNotificationId(),
                    builder.getNotification());*/
                Log.d(TAG, message + ". " + req.toString());

                AttachmentList att = publish(context, req, al);
                atts.add(att);
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

    private static AttachmentList publish(Context context,
                                          PutFeedItemsRequest req, AttachmentList al)
            throws ConnectionException {

        String feedName = req.getFeedName();
        String creatorUid = req.getCreatorUID();
        TakHttpClient client = null;
        try {
            client = TakHttpClient.GetHttpClient(req.getServerURL());

            String primaryResponseBody = null;
            if (req.isPutItem()) {
                //associate CoT UID with the feed
                String putUrl = client.getUrl("api/missions/" + feedName
                        + "/contents");
                if (!FileSystemUtils.isEmpty(creatorUid))
                    putUrl += "?creatorUid=" + creatorUid;
                putUrl = FileSystemUtils.sanitizeURL(putUrl);

                HttpPut put = new HttpPut(putUrl);
                put.addHeader("Content-Type", HttpUtil.MIME_JSON);
                put.addHeader("Accept-Encoding", HttpUtil.GZIP);

                // Add additional request headers
                addHeaders(put, req);

                StringEntity se = new StringEntity(
                        PutFeedItemsRequest.getRequestBody(al.getUid()));
                se.setContentType(HttpUtil.MIME_JSON);
                se.setContentEncoding(FileSystemUtils.UTF8_CHARSET.name());
                put.setEntity(se);

                Log.d(TAG, "Associating " + al.getUid() + " with feed "
                        + feedName + ", " + put.getRequestLine());
                TakHttpResponse response = client.execute(put);
                response.verifyOk();

                primaryResponseBody = response.getStringEntity(
                        RestManager.MATCHER_MISSION);
            }

            //now see if we should upload attachments
            List<String> attachmentResponses = new ArrayList<String>();
            if (al.hasLocalPaths() && al.hasHashes()) {
                //we need to combine all 3 so ensure they 1-1 in terms of size
                if (al.getHashes().size() != al.getLocalPaths().size()) {
                    throw new IOException("Failed to parse attachment list");
                }

                for (String attachmentPath : al.getLocalPaths()) {
                    //skip child directories
                    File attachment = new File(attachmentPath);
                    if (!FileSystemUtils.isFile(attachment)
                            || attachment.isDirectory()) {
                        Log.w(TAG, "Skipping invalid attachment: "
                                + attachmentPath);
                        continue;
                    }

                    String mimeType = ResourceFile.UNKNOWN_MIME_TYPE;
                    ResourceFile.MIMEType mt = ResourceFile
                            .getMIMETypeForFile(attachment.getAbsolutePath());
                    if (mt != null)
                        mimeType = mt.MIME;

                    //publish file and add response to match hash/local path we are already tracking
                    Pair<String, String> p = PutFeedFilesOperation.publish(
                            context, req, attachment, mimeType, null);
                    if (!FileSystemUtils.isEmpty(p.second)) {
                        Log.d(TAG, "Including file hash: " + p.first
                                + ", for CoT UID: " + al.getUid());
                        attachmentResponses.add(p.second);
                    } else {
                        throw new IOException("Unable to publish file: "
                                + attachment.getAbsolutePath());
                    }
                } //end attachment loop
            }

            return new AttachmentList(al.getUid(), primaryResponseBody,
                    al.getLocalPaths(), al.getHashes(), attachmentResponses);
        } catch (TakHttpException e) {
            Log.e(TAG, "Failed to put: " + feedName, e);
            throw new ConnectionException(e.getMessage(), e.getStatusCode());
        } catch (ConnectionException e) {
            Log.e(TAG, "Failed to put: " + feedName, e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Failed to put: " + feedName, e);
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
