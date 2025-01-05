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

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.atakmap.android.config.FiltersConfig;
import com.atakmap.android.icons.Icon2525bTypeResolver;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.icons.UserIconDatabase;
import com.atakmap.android.maps.AssetMapDataRef;
import com.atakmap.android.maps.MapDataRef;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.SqliteMapDataRef;
import com.atakmap.android.user.icon.Icon2525bPallet;
import com.atakmap.android.user.icon.SpotMapPallet;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.vehicle.model.VehicleModelCache;
import com.atakmap.android.vehicle.model.VehicleModelInfo;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for retrieving icons based on URIs, content types,
 * map item types, etc.
 */
public class IconUtils {

    private static Context _libCtx;

    // Cache type/icon path to icon URI
    private static FiltersConfig _iconFilters;
    private static final Map<String, String> _iconURIs = new HashMap<>();

    public static void init(Context libCtx) {
        _libCtx = libCtx;
    }

    static {
        // Icon filters
        MapView mv = MapView.getMapView();
        if (mv != null) {
            AssetManager assetMgr = mv.getContext().getAssets();
            try {
                _iconFilters = FiltersConfig.parseFromStream(assetMgr
                        .open("filters/icon_filters.xml"));
            } catch (Exception ignore) {
            }
        }

        // Map item type -> icon
        _iconURIs.put("b-i-v", getResourceURI(
                com.atakmap.app.R.drawable.ic_video_alias));
        _iconURIs.put("u-d-r", "asset://icons/rectangle.png");
        _iconURIs.put("u-d-f", getResourceURI(
                com.atakmap.app.R.drawable.shape));
        _iconURIs.put("u-d-f-m", getResourceURI(
                com.atakmap.app.R.drawable.multipolyline));
        _iconURIs.put("u-d-c-c", getResourceURI(
                com.atakmap.app.R.drawable.ic_circle));
        _iconURIs.put("u-r-b-c-c", getResourceURI(
                com.atakmap.app.R.drawable.ic_circle));
        _iconURIs.put("u-d-v", getResourceURI(
                com.atakmap.app.R.drawable.pointtype_aircraft));
        _iconURIs.put("u-rb-a", getResourceURI(
                com.atakmap.app.R.drawable.pairing_line_white));
        _iconURIs.put("u-r-b-bullseye", getResourceURI(
                com.atakmap.app.R.drawable.bullseye));
        _iconURIs.put("b-m-r", getResourceURI(
                com.atakmap.app.R.drawable.ic_route));
        _iconURIs.put("b-a-o-can", "asset://icons/alarm-trouble.png");
        _iconURIs.put("a-f-G", "asset://icons/friendly.png");
        _iconURIs.put("a-h-G", "asset://icons/target.png");
        _iconURIs.put("a-n-G", "asset://icons/neutral.png");
        _iconURIs.put("a-u-G", "asset://icons/unknown.png");
        _iconURIs.put("b-r-f-h-c", "asset://icons/damaged.png");
        _iconURIs.put("b-m-p-c", "asset://icons/reference_point.png");
    }

    /**
     * Get resource URI for drawable
     * @param ctx Drawable context
     * @param resId Drawable resource ID
     * @return String resource URI
     */
    public static String getResourceURI(Context ctx, int resId) {
        return "android.resource://" + ctx.getPackageName() + "/" + resId;
    }

    /**
     * Get ATAK drawable resource URI
     * @param resId ATAK resource ID
     * @return ATAK drawable resource URI
     */
    public static String getResourceURI(int resId) {
        MapView mv = MapView.getMapView();
        return mv != null ? getResourceURI(mv.getContext(), resId) : "";
    }

    /**
     * Retrieve icon URI using CoT type and icon path
     * @param type CoT type
     * @param iconPath Icon path (optional)
     * @return Icon URI string
     */
    public static String getIconURI(String type, String iconPath) {
        String key = type;
        if (!FileSystemUtils.isEmpty(iconPath))
            key += "\\" + iconPath;

        // Set default type icons
        synchronized (_iconURIs) {
            if (_iconURIs.containsKey(key))
                return _iconURIs.get(key);
        }

        // Find icon using path
        String iconUri = null;
        if (!FileSystemUtils.isEmpty(iconPath)) {

            MapView mv = MapView.getMapView();
            if (mv == null)
                return null;

            Context ctx = mv.getContext();

            // Find user-provided icon
            if (iconPath.startsWith(Icon2525bPallet.COT_MAPPING_2525B)) {
                // Find 2525b icon
                String type2525 = Icon2525bTypeResolver
                        .mil2525bFromCotType(type);
                if (!FileSystemUtils.isEmpty(type2525))
                    iconUri = AssetMapDataRef.toUri(
                            "mil-std-2525b/" + type2525 + ".png");
            } else if (iconPath.startsWith(SpotMapPallet.COT_MAPPING_SPOTMAP)) {
                // Find spot map icon (generic point or label)
                iconUri = AssetMapDataRef.toUri("icons/reference_point.png");
                if (iconPath.endsWith("LABEL"))
                    iconUri = getResourceURI(
                            com.atakmap.app.R.drawable.enter_location_label_icon);
            } else if (UserIcon.IsValidIconsetPath(iconPath, false, ctx)) {
                // Database icon
                String query = UserIcon.GetIconBitmapQueryFromIconsetPath(
                        iconPath, ctx);
                if (!FileSystemUtils.isEmpty(query)) {
                    MapDataRef iconRef = new SqliteMapDataRef(
                            UserIconDatabase.instance(ctx)
                                    .getDatabaseName(), query);
                    iconUri = iconRef.toUri();
                }
            }
        }

        // Find icon using type
        if (FileSystemUtils.isEmpty(iconUri)) {
            // Lookup default type icon
            if (_iconFilters != null && FileSystemUtils.isEmpty(iconUri)) {
                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put("type", type);
                FiltersConfig.Filter f = _iconFilters.lookupFilter(
                        metadata);
                String v = f != null ? f.getValue() : null;
                if (!FileSystemUtils.isEmpty(v))
                    iconUri = AssetMapDataRef.toUri(v);
            }
        }

        synchronized (_iconURIs) {
            //Log.d(TAG, key + " -> " + iconUri);
            _iconURIs.put(key, iconUri);
        }

        return iconUri;
    }

    /**
     * Get icon for a specific content type
     * @param fileName File name
     * @param contentType Content type
     * @return Drawable
     */
    public static Drawable getContentIcon(String fileName, String contentType) {
        MapView mv = MapView.getMapView();
        if (mv == null || FileSystemUtils.isEmpty(fileName)
                && FileSystemUtils.isEmpty(contentType))
            return null;


        Context ctx = mv.getContext();

        if (!FileSystemUtils.isEmpty(contentType)) {
            // Custom overrides
            if (contentType.equals("Video"))
                return ctx.getDrawable(com.atakmap.app.R.drawable.ic_video_alias);
            /*else if (contentType.equals(RemoteContentHandler.CONTENT_TYPE))
                return ctx.getDrawable(com.atakmap.app.R.drawable.download_remote_file);*/

            Drawable icon = ATAKUtilities.getContentIcon(contentType);
            if (icon != null)
                return icon;
        }

        File f = new File(fileName);
        if (FileUtils.isVideo(f))
            return ctx.getDrawable(com.atakmap.app.R.drawable.ic_video_alias);

        return ATAKUtilities.getFileIcon(f);
    }

    public static Drawable getCoreDrawable(int resId) {
        MapView mv = MapView.getMapView();
        if (mv == null)
            return null;
        return mv.getResources().getDrawable(resId);
    }

    public static Drawable getDrawable(int resId) {
       return _libCtx.getDrawable(resId);
    }

    /**
     * Get drawable from URI
     * @param iconURI Icon URI
     * @return Drawable
     */
    public static Drawable getIconDrawable(String iconURI) {
        if (iconURI == null)
            return null;

        if (iconURI.startsWith("model://")) {
            iconURI = iconURI.substring(iconURI.indexOf("://") + 3);
            String[] parts = iconURI.split("\\\\");
            if (parts.length < 3)
                return null;
            if (parts[0].equals("vehicle")) {
                VehicleModelInfo vehicle = VehicleModelCache.getInstance()
                        .get(parts[1], parts[2]);
                if (vehicle != null)
                    return vehicle.getIcon();
            }
        }
        Bitmap icon = ATAKUtilities.getUriBitmap(iconURI);
        return icon != null ? new BitmapDrawable(icon) : null;
    }
}
