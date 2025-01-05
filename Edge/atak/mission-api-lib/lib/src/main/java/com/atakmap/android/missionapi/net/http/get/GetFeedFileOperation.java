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

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.atakmap.android.missionapi.data.FileChangeWatcher;
import com.atakmap.android.missionapi.model.json.FeedFileDetails;
import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.PendingRestOperation;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.RetryRequest.OperationDelay;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.android.missionapi.notifications.NotificationBuilder;
import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.http.rest.DownloadProgressTracker;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.math.MathUtils;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpClient;
import com.atakmap.comms.http.TakHttpResponse;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.filesystem.HashingUtils;
import com.foxykeep.datadroid.exception.ConnectionException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by byoung on 4/12/2016.
 */
public class GetFeedFileOperation extends AbstractOperation {

    private static final String TAG = "GetFeedFileOperation";

    //TODO need to tweak these?
    //private static long DEFAULT_CONNECTION_TIMEOUT_MS = 1000;
    //private static long DEFAULT_TRANSFER_TIMEOUT_MS = 10000;

    /**
     * Hack! Since DataDroid does not re-parcel the request upon error, rather it uses the instance
     * cached by HTTPRequestManager, this operation is unable to modify the metadata associated with
     * the request upon error (failed download). Therefore we use a special status code to indicate
     * to the listener that some progress was made, but still hit an error. The request is
     * re-parceled upon success
     */
    public static final int PROGRESS_MADE_STATUS_CODE = 9999;
    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException {

        // Get request data
        GetFeedFileRequest req = (GetFeedFileRequest) r;

        String filepath = getFile(context, req.getDelay(),
                req.getDetails(), req.getFeedName(), req.getFeedUID(),
                req.getServerURL(), req.getNotificationID());

        // Move to custom directory if specified
        File dir = req.getDestinationDirectory();
        if (dir != null) {
            File src = new File(filepath);
            File dst = new File(dir, src.getName());
            if (FileSystemUtils.renameTo(src, dst))
                filepath = dst.getAbsolutePath();
        }

        b.putString(RestManager.RESPONSE_JSON, filepath);
    }

    public static String getFile(Context context, OperationDelay delay,
             FeedFileDetails details, String feedName, String feedUID,
             String baseURL, int notificationId) throws ConnectionException {
        File temp = null;
        File dest = null;
        try {
            // Directory where temp files are stored
            File tempDir = FileSystemUtils.getItem(
                    FeedUtils.INCOMING_FILE_FOLDER
                            + File.separatorChar + feedUID);
            tempDir.mkdirs();
            temp = new File(tempDir, details.getHash());

            // Directory where the file is moved after download is complete
            File destDir = FileSystemUtils.getItem(
                    FeedUtils.DATA_FOLDER + File.separatorChar
                            + feedUID + File.separatorChar
                            + details.getHash());
            destDir.mkdirs();
            dest = new File(destDir, details.getName());

            // Disable prompts for these 2 files
            FileChangeWatcher.getInstance().ignore(temp);
            FileChangeWatcher.getInstance().ignore(dest);

            return getFileImpl(context, delay, details, feedName, feedUID,
                    baseURL, notificationId, temp, dest);
        } finally {
            // Un-ignore files above
            if (temp != null)
                FileChangeWatcher.getInstance().unignore(temp);
            if (dest != null)
                FileChangeWatcher.getInstance().unignore(dest);
        }
    }

    private static String getFileImpl(Context context, OperationDelay delay,
            FeedFileDetails details, String feedName,
            String feedUID, String baseUrl, int notificationId,
            File temp, File dest) throws ConnectionException {

        // setup progress notifications
        String tickerFilename = details.getName();
        NotificationManager notifyManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationBuilder builder = new NotificationBuilder();
        //TODO update context text once we have file metadata
        builder.setContentTitle("File Download")
                .setContentText("Downloading " + feedName + " file: "
                        + tickerFilename)
                .setSmallIcon(
                        NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID());

        String opUID = PendingRestOperation.generateUID(feedUID,
                PendingRestOperation.OpType.DOWNLOAD.toString(),
                details.getHash());
        PendingRestOperation op = RestManager.getInstance()
                .getPendingOperation(opUID);

        int statusCode = NetworkOperation.STATUSCODE_UNKNOWN;
        DownloadProgressTracker progressTracker = null;
        TakHttpClient client = null;
        try {
            // delay this operation if it has previously failed
            if (delay != null)
                delay.delay();

            // now start timer
            long startTime = System.currentTimeMillis();

            if (op != null)
                op.onComplete();

            // Create temp file
            boolean bRestart = false;
            long existingLength = 0;
            long totalFileSize = details.getSize();

            // this is a retry, lets see if we pick up where previous attempt left off
            if (temp.exists()) {
                if (temp.length() > 0 && temp.canWrite()
                        && temp.length() < totalFileSize) {
                    existingLength = temp.length();
                    bRestart = true;
                    Log.d(TAG, "Restarting download: " + details.getName()
                            + " after byte: " + existingLength);
                } else{
                    FileSystemUtils.deleteFile(temp);
                }
            }

            // ///////
            // Get file
            // ///////
            client = TakHttpClient.GetHttpClient(baseUrl);

            //TODO update progress/notification?
            String hashUrl = client.getUrl("sync/content");
            Uri.Builder uribuilder = Uri.parse(hashUrl).buildUpon();
            uribuilder.appendQueryParameter("hash", details.getHash());

            if (bRestart)
                uribuilder.appendQueryParameter("offset",
                        String.valueOf(existingLength));

            hashUrl = uribuilder.build().toString();
            hashUrl = FileSystemUtils.sanitizeURL(hashUrl);

            HttpGet httpget = new HttpGet(hashUrl);
            //for processing load/time efficiency do not need GZip for common data types that
            //are inherently compressed
            if (!isCompressedFormat(details)) {
                Log.d(TAG, "Accepting GZip for: " + details.getMimeType());
                httpget.addHeader("Accept-Encoding", HttpUtil.GZIP);
            }
            TakHttpResponse response = client.execute(httpget);
            statusCode = response.getStatusCode();
            response.verifyOk();

            // open up for writing
            FileOutputStream fos = new FileOutputStream(temp, bRestart);

            // stream in content, keep user notified on progress
            if (notificationId > 0) {
                builder.setProgress(1, 100);
                notifyManager.notify(notificationId, builder.build());
            }

            int len = 0;
            byte[] buf = new byte[8192];
            progressTracker = new DownloadProgressTracker(totalFileSize);
            // if this is a restart, update initial content length
            progressTracker.setCurrentLength(existingLength);

            HttpEntity resEntity = response.getEntity();
            InputStream in = resEntity.getContent();
            while ((len = in.read(buf)) > 0) {
                fos.write(buf, 0, len);

                // see if we should update progress notification based on progress or time since
                // last update
                long currentTime = System.currentTimeMillis();
                if (progressTracker.contentReceived(len, currentTime)) {
                    String message = String.format(LocaleUtil.getCurrent(),
                            "Downloading %s (%d%% of %s) %s, %s remaining",
                            tickerFilename,
                            progressTracker.getCurrentProgress(),
                            MathUtils.GetLengthString(totalFileSize),
                            MathUtils.GetDownloadSpeedString(progressTracker
                                    .getAverageSpeed()),
                            MathUtils.GetTimeRemainingString(progressTracker
                                    .getTimeRemaining()));
                    if (notificationId > 0) {
                        builder.setProgress(progressTracker.getCurrentProgress(), 100);
                        builder.setContentText(message);
                        notifyManager.notify(notificationId, builder.build());
                    }
                    Log.d(TAG, message);
                    // start a new block
                    progressTracker.notified(currentTime);
                    if (op != null)
                        op.onProgress(progressTracker);
                }
            } // end read loop
            in.close();
            fos.close();

            // Now verify we got download correctly
            if (!FileSystemUtils.isFile(temp)) {
                progressTracker.error();
                FileSystemUtils.deleteFile(temp);
                throw new ConnectionException("Failed to download data");
            }

            if (!HashingUtils.verify(temp, totalFileSize, details.getHash())) {
                progressTracker.error();
                FileSystemUtils.deleteFile(temp);

                throw new ConnectionException("Size or MD5 mismatch");
            }

            //now move file out of incoming, rename prior to processing
            final long downloadSize = temp.length();
            if (!FileSystemUtils.renameTo(temp, dest)) {
                progressTracker.error();
                FileSystemUtils.deleteFile(temp);

                throw new ConnectionException("Failed to rename file");
            }

            // update notification
            String message = String.format("Processing %s file: %s...",
                    feedName, tickerFilename);
            Log.d(TAG, "File Transfer downloaded and verified. " + message);

            if (notificationId > 0) {
                builder.setProgress(99, 100);
                builder.setContentText(message);
                notifyManager.notify(notificationId, builder.build());
            }

            //TODO add to manifest here or in listener/callback?
            //TODO import data, attach to marker or log? use Import Manager?

            long stopTime = System.currentTimeMillis();
            if (bRestart)
                Log.d(TAG,
                        String.format(
                                "Feed file restart processed %d of %d bytes in %f seconds",
                                (downloadSize - existingLength), downloadSize,
                                (stopTime - startTime) / 1000F));
            else
                Log.d(TAG, String.format(
                        "Feed file processed %d bytes in %f seconds",
                        downloadSize, (stopTime - startTime) / 1000F));

            return dest.getAbsolutePath();
        } catch (Exception e) {
            boolean bProgress = progressTracker != null
                    && progressTracker.isProgressMade();
            Log.e(TAG, "Failed to download feed file, progress made="
                    + bProgress, e);

            if (bProgress)
                throw new ConnectionException(e.getMessage(),
                        PROGRESS_MADE_STATUS_CODE);
            else
                throw new ConnectionException(e.getMessage(), statusCode);
        } finally {
            try {
                if (client != null)
                    client.shutdown();
            } catch (Exception ignore) {
            }
            if (op != null)
                op.onComplete();
        }
    }

    /**
     * Do not request GZip for a few common known compressed file formats
     *
     * @param details
     * @return
     */
    private static boolean isCompressedFormat(FeedFileDetails details) {
        //TODO expand on this method, and move into a Util somewhere? mabye in ResourceFile.java
        if (details == null)
            return false;

        ResourceFile.MIMEType mime = ResourceFile
                .getMIMETypeForMIME(details.getMimeType());
        if (mime == null)
            return false;

        switch (mime) {
            case JPG:
            case JPEG:
            case PNG:
            case GIF:
            case MPG:
            case WMV:
            case MOV:
            case MP3:
            case MP4:
            case ZIP:
            case KMZ:
            case APK:
                return true;
            default:
                return false;
        }
    }
}
