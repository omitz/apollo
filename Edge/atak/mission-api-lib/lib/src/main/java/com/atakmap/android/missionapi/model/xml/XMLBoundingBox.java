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
import org.simpleframework.xml.Element;

/**
 * Parcelable BoundingBox containing NW & SE values
 * 
 * @author byoung
 */
public class XMLBoundingBox {

    @Element(name = "NW", required = true)
    private XMLPoint mNW;

    @Element(name = "SE", required = true)
    private XMLPoint mSE;

    public XMLBoundingBox(XMLBoundingBox bbox) {
        this(bbox.getNW(), bbox.getSE());
    }

    public XMLBoundingBox(XMLPoint nw, XMLPoint se) {
        mNW = nw;
        mSE = se;
    }

    public XMLPoint getNW() {
        return mNW;
    }

    public XMLPoint getSE() {
        return mSE;
    }

    public boolean isValid() {
        return mNW != null && mNW.isValid() && mSE != null && mSE.isValid();
    }

    /**
     * (NW Lat, NW Lon) (SE Lat, SE Lon)
     */
    public String toString() {
        if (!isValid())
            return super.toString();

        return String.format(LocaleUtil.getCurrent(), "NW=(%s) SE=(%s)",
                mNW.toString(),
                mSE.toString());
    }

    public JSONObject toJSON() throws JSONException {
        if (!isValid())
            throw new JSONException("Invalid BoundingBox");

        JSONObject json = new JSONObject();
        json.put("NW", mNW.toJSON());
        json.put("SE", mSE.toJSON());
        return json;
    }

    public static XMLBoundingBox fromJSON(JSONObject json) throws JSONException {
        return new XMLBoundingBox(
                XMLPoint.fromJSON(json.getJSONObject("NW")),
                XMLPoint.fromJSON(json.getJSONObject("SE")));
    }

    @Override
    public int hashCode() {
        int result = mNW.hashCode();
        result = 31 * result + mSE.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XMLBoundingBox) {
            XMLBoundingBox c = (XMLBoundingBox) o;
            return this.equals(c);
        } else {
            return super.equals(o);
        }
    }

    public boolean equals(XMLBoundingBox c) {
        if (!isValid() || !c.isValid())
            return false;

        if ((mNW == null) != (c.mNW == null))
            return false;
        if (!mNW.equals(c.mNW))
            return false;

        if ((mSE == null) != (c.mSE == null))
            return false;
        if (!mSE.equals(c.mSE))
            return false;

        return true;
    }
}
