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

package com.atakmap.android.missionapi.model.json;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.atakmap.android.cot.CotUtils;
import com.atakmap.android.missionapi.model.xml.XMLContentUidDetails;
import com.atakmap.android.missionapi.util.IconUtils;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.Objects;

/**
 * CoT details
 */
public class FeedCoTDetails implements JSONSerializable {

    public String title, type, iconsetPath, iconURI;
    public String modelCategory, modelName;
    public int color = Color.WHITE;
    public GeoPoint location;

    public FeedCoTDetails() {
    }

    public FeedCoTDetails(JSONData data) {
        this.title = data.get("callsign", data.get("title", ""));
        this.type = data.get("type");
        this.color = data.get("color", Color.WHITE);
        this.iconsetPath = data.get("iconsetPath");
        this.location = parseLocation(data);

        // Model specific
        this.modelCategory = data.get("category");
        this.modelName = data.get("name");

        // Icon URI is generated last
        this.iconURI = getIconURI();
    }

    public FeedCoTDetails(CotEvent event) {
        this.type = event.getType();

        // Title and color
        this.title = CotUtils.getCallsign(event);
        this.color = findColor(event);

        // Location
        this.location = event.getGeoPoint();

        // Marker icon
        CotDetail iconDetail = event.findDetail("usericon");
        if (iconDetail != null)
            this.iconsetPath = iconDetail.getAttribute("iconsetpath");

        // Vehicle model
        CotDetail model = event.findDetail("model");
        if (model != null) {
            String type = model.getAttribute("type");
            if (type != null && type.equals("vehicle")) {
                modelCategory = model.getAttribute("category");
                modelName = model.getAttribute("name");
            }
        }

        this.iconURI = getIconURI();
    }

    public FeedCoTDetails(FeedCoTDetails other) {
        this.title = other.title;
        this.type = other.type;
        this.iconsetPath = other.iconsetPath;
        this.iconURI = other.iconURI;
        this.modelCategory = other.modelCategory;
        this.modelName = other.modelName;
        this.color = other.color;
        this.location = other.location;
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData details = new JSONData(server);
        details.set("callsign", this.title);
        details.set("type", this.type);
        details.set("color", this.color);
        details.set("iconsetPath", this.iconsetPath);
        details.set("location", locationToJSON(this.location, server));

        // Vehicle model
        details.set("category", this.modelCategory);
        details.set("name", this.modelName);

        return details;
    }

    private String getIconURI() {
        if (modelName != null & modelCategory != null) {
            String modelType = null;
            if (type.equals("u-d-v-m"))
                modelType = "vehicle";
            if (modelType != null)
                return "model://" + modelType + "\\" + modelCategory + "\\" + modelName;
        }
        return IconUtils.getIconURI(this.type, this.iconsetPath);
    }

    public Drawable getIconDrawable() {
        return IconUtils.getIconDrawable(this.iconURI);
    }

    public FeedCoTDetails(XMLContentUidDetails xml) {
        this.title = xml.callsign != null ? xml.callsign : xml.title;
        this.type = xml.type;
        if (xml.color != null)
            this.color = xml.color;
        this.iconsetPath = xml.iconsetPath;
        this.modelCategory = xml.category;
        this.modelName = xml.name;
        this.iconURI = getIconURI();
        this.location = xml.getLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FeedCoTDetails that = (FeedCoTDetails) o;
        return color == that.color &&
                Objects.equals(title, that.title) &&
                Objects.equals(type, that.type) &&
                Objects.equals(location, that.location) &&
                Objects.equals(iconsetPath, that.iconsetPath) &&
                Objects.equals(iconURI, that.iconURI) &&
                Objects.equals(modelCategory, that.modelCategory) &&
                Objects.equals(modelName, that.modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, type, location, iconsetPath, iconURI,
                modelCategory, modelName, color);
    }

    private static Integer findColor(CotEvent event) {
        Integer colorInt = parseColor(event, "color", "argb");
        if (colorInt == null)
            colorInt = parseColor(event, "color", "value");
        if (colorInt == null)
            colorInt = parseColor(event, "strokeColor", "value");
        if (colorInt == null)
            colorInt = parseColor(event, "link_attr", "color");
        return colorInt != null ? colorInt : Color.WHITE;
    }

    private static Integer parseColor(CotEvent event, String key,
                                      String value) {
        CotDetail color = event.findDetail(key);
        if (color != null) {
            try {
                return Integer.parseInt(color.getAttribute(value));
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    /**
     * Parse a geo point from JSON location data
     * @param data JSON data
     * @return Geo point or null if failed
     */
    private static GeoPoint parseLocation(JSONData data) {
        JSONData loc = data.getChild("location");
        if (loc == null)
            return null;
        return new GeoPoint(loc.get("lat", Double.NaN), loc.get("lon", Double.NaN));
    }

    /**
     * Converts a geo point to JSON location data
     * @param point Point
     * @param server True if serializing for server use
     * @return JSON data
     */
    private static JSONData locationToJSON(GeoPoint point, boolean server) {
        if (point == null || !point.isValid())
            return null;
        JSONData d = new JSONData(server);
        d.set("lat", point.getLatitude());
        d.set("lon", point.getLongitude());
        return d;
    }
}
