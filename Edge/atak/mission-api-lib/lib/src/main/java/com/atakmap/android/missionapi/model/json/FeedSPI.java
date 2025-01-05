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

import com.atakmap.coremap.log.Log;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manage SPIs related to a feed or its contents
 */
public class FeedSPI {

    private static final String TAG = "FeedSPI";

    private static final FeedSPI _instance = new FeedSPI();

    public static FeedSPI getInstance() {
        return _instance;
    }

    // SPIs for the Feed class
    private final List<Class<? extends Feed>> _feedSPIs;

    // SPIs for bottom-level feed contents (not extended within this library)
    private final Map<Class<?>, List<Class<?>>> _contentSPIs;

    // Constructor cache
    private final Map<String, Constructor<?>> _ctors;

    private FeedSPI() {
        _feedSPIs = new ArrayList<>();
        _contentSPIs = new HashMap<>();
        _ctors = new HashMap<>();
    }

    public synchronized void addFeedSPI(Class<? extends Feed> subClass) {
        _feedSPIs.add(subClass);
    }

    public synchronized void removeFeedSPI(Class<? extends Feed> subClass) {
        _feedSPIs.remove(subClass);
    }

    public synchronized void addContentSPI(Class<?> baseClass,
            Class<?> subClass) {
        List<Class<?>> classes = _contentSPIs.get(baseClass);
        if (classes == null)
            _contentSPIs.put(baseClass, classes = new ArrayList<>());
        classes.add(subClass);
    }

    public synchronized void removeContentSPI(Class<?> baseClass,
            Class<?> subClass) {
        List<Class<?>> classes = _contentSPIs.get(baseClass);
        if (classes != null) {
            classes.remove(subClass);
            if (classes.isEmpty())
                _contentSPIs.remove(baseClass);
        }
    }

    public Feed createFeed(Object... args) {
        List<Class<? extends Feed>> spis;
        synchronized (this) {
            spis = new ArrayList<>(_feedSPIs);
        }
        for (Class<? extends Feed> spi : spis) {
            Feed feed = create(spi, args);
            if (feed != null)
                return feed;
        }
        return create(Feed.class, args);
    }

    public <T extends FeedContent> T createContent(Class<T> clazz,
            Object... args) {
        List<Class<?>> spis;
        synchronized (this) {
            spis = _contentSPIs.get(clazz);
        }
        if (spis != null) {
            for (Class<?> spi : spis) {
                T content = create(spi, args);
                if (content != null)
                    return content;
            }
        }
        return create(clazz, args);
    }

    @SuppressWarnings("unchecked")
    private <T> T create(Class<?> cl, Object... args) {
        Class<?>[] clArgs = new Class<?>[args.length];
        try {
            for (int i = 0; i < args.length; i++)
                clArgs[i] = args[i].getClass();
            Constructor<?> ctor = getConstructor(cl, clArgs);
            return (T) ctor.newInstance(args);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create new instance of " + cl.getName()
                    + Arrays.toString(clArgs).replace('[', '(').replace(']', ')'));
            return null;
        }
    }

    public Constructor<?> getContentCtor(Class<?> cl, Class<?>... args) {
        List<Class<?>> spis;
        synchronized (this) {
            spis = _contentSPIs.get(cl);
        }
        if (spis != null) {
            for (Class<?> spi : spis) {
                Constructor<?> ctor = getConstructor(spi, args);
                if (ctor != null)
                    return ctor;
            }
        }
        return getConstructor(cl ,args);
    }

    public Constructor<?> getConstructor(Class<?> cl, Class<?>... args) {
        String key = cl.getName() + "(" + Arrays.toString(args) + ")";
        synchronized (this) {
            Constructor<?> ctor = _ctors.get(key);
            if (ctor != null)
                return ctor;
        }
        try {
            Constructor<?>[] ctors = cl.getDeclaredConstructors();
            outer: for (Constructor<?> ctor : ctors) {
                Class<?>[] types = ctor.getParameterTypes();
                if (types.length != args.length)
                    continue;
                for (int i = 0; i < types.length; i++) {
                    if (!types[i].isAssignableFrom(args[i]))
                        continue outer;
                }
                ctor.setAccessible(true);
                synchronized (this) {
                    _ctors.put(key, ctor);
                }
                return ctor;
            }
        } catch (Exception ignored) {
        }
        Log.e(TAG, "Failed to get constructor for " + cl.getName()
                + Arrays.toString(args).replace('[', '(').replace(']', ')'));
        return null;
    }
}
