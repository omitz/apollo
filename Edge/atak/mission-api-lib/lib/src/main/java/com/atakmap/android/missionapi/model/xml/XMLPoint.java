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

package com.atakmap.android.missionapi.model.xml;

import com.atakmap.coremap.locale.LocaleUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.simpleframework.xml.Attribute;

/**
 * Parcelable Point containing Lat/Lon values
 * 
 * @author byoung
 */
public class XMLPoint {

    @Attribute(name = "lat", required = true)
    private double mLat;

    @Attribute(name = "lon", required = true)
    private double mLon;

    public XMLPoint() {
    }

    public XMLPoint(double lat, double lon) {
        mLat = lat;
        mLon = lon;
    }

    public double getLat() {
        return mLat;
    }

    public double getLon() {
        return mLon;
    }

    public boolean isValid() {
        //TODO check in valid lat/lon range
        return !Double.isNaN(mLat) && !Double.isNaN(mLon);
    }

    public String toString() {
        return String.format(LocaleUtil.getCurrent(), "%.4f %.4f", mLat, mLon);
    }

    public JSONObject toJSON() throws JSONException {
        if (!isValid())
            throw new JSONException("Invalid Point");

        JSONObject json = new JSONObject();
        json.put("Lat", mLat);
        json.put("Lon", mLon);
        return json;
    }

    public static XMLPoint fromJSON(JSONObject json) throws JSONException {
        return new XMLPoint(
                json.getDouble("Lat"),
                json.getDouble("Lon"));
    }

    @Override
    public int hashCode() {
        if (!isValid())
            return super.hashCode();

        int result;
        long temp;
        temp = Double.doubleToLongBits(mLat);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mLon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XMLPoint) {
            XMLPoint c = (XMLPoint) o;
            return this.equals(c);
        } else {
            return super.equals(o);
        }
    }

    public boolean equals(XMLPoint c) {

        if (mLat != c.mLat)
            return false;

        if (mLon != c.mLon)
            return false;

        return true;
    }
}
