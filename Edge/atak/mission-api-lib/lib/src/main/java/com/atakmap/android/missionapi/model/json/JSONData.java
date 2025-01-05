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

import com.atakmap.android.missionapi.util.TimeUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Helpful struct for JSON data of any type (even arrays)
 * Tones down the exception panics and provides helper methods
 */
public class JSONData implements Iterable<JSONData> {

    private static final String TAG = "JSONData";

    private JSONObject _obj;
    private JSONArray _arr;
    private Object _prim;
    private boolean _server;

    public JSONData(boolean server) {
        this(new JSONObject(), server);
    }

    public JSONData(JSONObject obj, boolean server) {
        _obj = obj;
        _server = server;
    }

    public JSONData(JSONArray arr, boolean server) {
        _arr = arr;
        _server = server;
    }

    public JSONData(Object o, boolean server) {
        _server = server;
        if (o instanceof String) {
            String json = (String) o;
            try {
                _obj = new JSONObject(json);
            } catch (Exception e) {
                try {
                    _arr = new JSONArray(json);
                } catch (Exception e2) {
                    _prim = json;
                }
            }
        } else if (o instanceof JSONObject)
            _obj = (JSONObject) o;
        else if (o instanceof JSONArray)
            _arr = (JSONArray) o;
        else if (o instanceof JSONData) {
            JSONData data = (JSONData) o;
            _obj = data._obj;
            _arr = data._arr;
            _prim = data._prim;
        } else
            _prim = o;
    }

    /**
     * Whether this data is meant to be read/written server-side
     * @return True if server data
     */
    public boolean isServerData() {
        return _server;
    }

    public int length() {
        if (_obj != null)
            return _obj.length();
        else if (_arr != null)
            return _arr.length();
        return _prim != null ? 1 : 0;
    }

    public boolean has(String key) {
        return _obj != null && _obj.has(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) getVar(key, null);
    }

    public String get(String key, String value) {
        return (String) getVar(key, value);
    }

    public int get(String key, int defVal) {
        return (Integer) getVar(key, defVal);
    }

    public long get(String key, long defVal) {
        return (Long) getVar(key, defVal);
    }

    public double get(String key, double defVal) {
        return (Double) getVar(key, defVal);
    }

    public boolean get(String key, boolean defVal) {
        return (Boolean) getVar(key, defVal);
    }

    public long getTimestamp(String key) {
        return TimeUtils.parseTimestampMillis(get(key, ""));
    }

    public JSONData getChild(String key) {
        Object o = get(key);
        if (o instanceof JSONObject)
            return new JSONData((JSONObject) o, _server);
        else if (o instanceof JSONArray)
            return new JSONData((JSONArray) o, _server);
        return new JSONData(o, _server);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, T defVal) {
        JSONArray arr = (JSONArray) getVar(key, new JSONArray());
        JSONData dataArr = new JSONData(arr, _server);
        List<T> ret = new ArrayList<>(arr.length());
        for (JSONData d : dataArr)
            ret.add((T) getAs(d._prim, defVal));
        return ret;
    }

    public <T> List<T> getList(String key, Feed feed, Class<T> clazz) {
        JSONArray arr = (JSONArray) getVar(key, new JSONArray());
        List<T> ret = new ArrayList<>(arr.length());
        Method method = null;
        Constructor<?> ctor = null;
        try {
            method = clazz.getMethod("fromJSON", Feed.class, JSONData.class);
        } catch (NoSuchMethodException e1) {
            try {
                ctor = FeedSPI.getInstance().getContentCtor(clazz,
                        Feed.class, JSONData.class);
            } catch (Exception e) {
                Log.d(TAG, "Failed to find JSON data constructor", e);
                return ret;
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to convert JSON data method", e);
        }
        try {
            JSONData dataArr = new JSONData(arr, _server);
            for (JSONData d : dataArr) {
                T obj = null;
                if (method != null)
                    obj = (T) method.invoke(clazz, feed, d);
                else if (ctor != null)
                    obj = (T) ctor.newInstance(feed, d);
                if (obj != null)
                    ret.add(obj);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to instantiate " + clazz, e);
        }

        return ret;
    }

    public void set(String key, Object value) {
        try {
            if (_obj != null && value != null) {
                if (value instanceof Collection) {
                    Collection<?> coll = (Collection<?>) value;
                    JSONArray arr = new JSONArray();
                    for (Object o : coll)
                        arr.put(toJSON(o));
                    _obj.put(key, arr);
                } else
                    _obj.put(key, toJSON(value));
            }
        } catch (Exception ignored) {
        }
    }

    public void remove(String key) {
        if (_obj != null)
            _obj.remove(key);
    }

    public String asString(String defVal) {
        return (String) getAs(_prim, defVal);
    }

    public String asString() {
        return asString("");
    }

    public int asInt(int defVal) {
        return (Integer) getAs(_prim, defVal);
    }

    public int asInt() {
        return asInt(-1);
    }

    public double asDouble(double defVal) {
        return (Double) getAs(_prim, defVal);
    }

    public double asDouble() {
        return asDouble(Double.NaN);
    }

    public boolean asBoolean() {
        return (Boolean) getAs(_prim, false);
    }

    private Object toJSON(Object o) {
        if (o instanceof JSONSerializable)
            o = ((JSONSerializable) o).toJSON(_server);
        if (o instanceof JSONData) {
            JSONData data = (JSONData) o;
            if (data._obj != null)
                return data._obj;
            else if (data._arr != null)
                return data._arr;
            else
                return data._prim;
        }
        return o;
    }

    public Object toJSON() {
        return toJSON(this);
    }

    public JSONObject asJSONObject() {
        return _obj;
    }

    public JSONArray asJSONArray() {
        return _arr;
    }

    public boolean isArray() {
        return _arr != null;
    }

    public boolean isObject() {
        return _obj != null;
    }

    private Object getVar(String key, Object d) {
        if (_obj == null || !_obj.has(key))
            return d;
        try {
            return getAs(_obj.get(key), d);
        } catch (Exception ignored) {
        }
        return d;
    }

    private static Object getAs(Object o, Object d) {
        try {
            if (o == null || o == JSONObject.NULL)
                return d;
            if (d == null || d.getClass().isInstance(o))
                return o;

            // Attempt a string cast
            String str = String.valueOf(o);
            if (d instanceof String)
                return str;
            else if (d instanceof Integer)
                return Integer.parseInt(str);
            else if (d instanceof Long)
                return Long.parseLong(str);
            else if (d instanceof Float)
                return Float.parseFloat(str);
            else if (d instanceof Double)
                return Double.parseDouble(str);
            else if (d instanceof Boolean)
                return Boolean.parseBoolean(str);
        } catch (Exception ignored) {
        }
        return d;
    }

    @Override
    public String toString() {
        if (_obj != null)
            return _obj.toString();
        else if (_arr != null)
            return _arr.toString();
        return String.valueOf(_prim);
    }

    public String toString(int indentSpaces) {
        try {
            if (_obj != null)
                return _obj.toString(indentSpaces);
            else if (_arr != null)
                return _arr.toString(indentSpaces);
            return String.valueOf(_prim);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    @NonNull
    public Iterator<JSONData> iterator() {
        return new JSONArrayIterator();
    }

    private class JSONArrayIterator implements Iterator<JSONData> {

        private int _index;

        @Override
        public boolean hasNext() {
            return _arr != null && _index < _arr.length();
        }

        @Override
        public JSONData next() {
            try {
                if (_arr != null) {
                    Object o = _arr.get(_index);
                    if (o instanceof JSONObject)
                        return new JSONData((JSONObject) o, _server);
                    else if (o instanceof JSONArray)
                        return new JSONData((JSONArray) o, _server);
                    else
                        return new JSONData(o, _server);
                }
            } catch (Exception e) {
                return null;
            } finally {
                _index++;
            }
            return null;
        }
    }
}
