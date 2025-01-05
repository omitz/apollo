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

package com.atakmap.android.missionapi.util;

import android.util.Pair;

import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.filesystem.ResourceFile.MIMEType;
import com.atakmap.android.importfiles.sort.ImportResolver;
import com.atakmap.android.importfiles.task.ImportFilesTask;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.video.manager.VideoFileWatcher;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileUtils {

    private static final String TAG = "FileUtils";

    /**
     * Get file extension
     * @param fileName File name
     * @return Extension (lowercase)
     */
    public static String getExtension(String fileName) {
        if (FileSystemUtils.isEmpty(fileName))
            return null;
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx == -1)
            return null;
        return fileName.substring(dotIdx + 1).toLowerCase();
    }

    /**
     * Check if a file is a regular viewable image (not imagery)
     * @param f File to check
     * @return True if the file is an image
     */
    public static boolean isImage(File f) {
        if (f == null)
            return false;
        MIMEType mime = ResourceFile.getMIMETypeForFile(f.getName());
        return mime == MIMEType.JPG || mime == MIMEType.JPEG
                || mime == MIMEType.PNG || mime == MIMEType.BMP
                || mime == MIMEType.GIF;
    }

    /**
     * Check if a file is a video
     * @param f File to check
     * @return True if the file is a video
     */
    public static boolean isVideo(File f) {
        if (f == null)
            return false;
        String ext = getExtension(f.getName());
        if (FileSystemUtils.isEmpty(ext))
            return false;
        if (ext.equals("xml")) {
            // Only considered a video XML if it's under the /atak/tools/videos dir
            return f.getParent() != null
                    && f.getParent().contains("/atak/tooks/videos");
        }
        return VideoFileWatcher.VIDEO_EXTS.contains(ext);
    }

    /**
     * Check if a file is a feature set based on its extension
     * @param f File to check
     * @return True if the file is a feature data store
     */
    public static boolean isFeatureSet(File f) {
        if (f == null)
            return false;
        MIMEType mime = ResourceFile.getMIMETypeForFile(f.getName());
        return mime == MIMEType.KML || mime == MIMEType.KMZ
                || mime == MIMEType.SHP || mime == MIMEType.SHPZ
                || mime == MIMEType.GPX;
    }

    /**
     * Check if a file is a file database based on its extension
     * @param f File to check
     * @return True if the file is a file database
     */
    public static boolean isFileDatabase(File f) {
        if (f == null)
            return false;
        MIMEType mime = ResourceFile.getMIMETypeForFile(f.getName());
        return mime == MIMEType.DRW || mime == MIMEType.LPT;
    }

    /**
     * Check if a file is a text file based on its extension
     * @param f File to check
     * @return True if the file is an ext file
     */
    public static boolean isTextFile(File f) {
        if (f == null)
            return false;
        MIMEType mime = ResourceFile.getMIMETypeForFile(f.getName());
        return mime == MIMEType.TXT || mime == MIMEType.CSV
                || mime == MIMEType.XML || f.getName().endsWith(".json");
    }

    public static List<ImportResolver> findSorters(File f, boolean forDisplay) {
        List<ImportResolver> ret = new ArrayList<>();
        MapView mv = MapView.getMapView();
        if (mv == null)
            return ret;

        List<ImportResolver> sorters = ImportFilesTask.GetSorters(
                mv.getContext(), true, false, true, false);
        Set<String> names = new HashSet<>();
        for (ImportResolver res : sorters) {
            if (res.getContentMIME() == null) {
                //Log.w(TAG, "Removing invalid sorter: " + res.getDisplayableName());
                continue;
            }
            if (res.match(f)) {

                // Displaying a list of content types - do not include duplicates
                if (forDisplay) {
                    String name = res.getDisplayableName();
                    if (names.contains(name))
                        continue;
                    else
                        names.add(name);
                }

                ret.add(res);
            }
        }
        return ret;
    }

    public static List<ImportResolver> findSorters(File f) {
        return findSorters(f, false);
    }

    public static ImportResolver findSorter(File f, String contentType) {
        List<ImportResolver> sorters = findSorters(f);
        for (ImportResolver sorter : sorters) {
            Pair<String, String> mt = sorter.getContentMIME();
            if (mt != null && FileSystemUtils.isEquals(contentType, mt.first))
                return sorter;
        }
        return null;
    }
}
