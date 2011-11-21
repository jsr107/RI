/**
 *  Copyright 2011 Terracotta, Inc.
 *  Copyright 2011 Oracle America Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.cache.implementation;

import javax.cache.Cache;
import javax.cache.CacheBuilder;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.OptionalFeature;
import javax.cache.Status;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The reference implementation for JSR107.
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 */
public class RICacheManager extends AbstractCacheManager implements CacheManager {

    private static final Logger LOGGER = Logger.getLogger("javax.cache");
    private final HashMap<String, Cache<?, ?>> caches = new HashMap<String, Cache<?, ?>>();
    private volatile Status status;

    /**
     * Constructs a new RICacheManager with the specified name.
     *
     * @param classLoader the ClassLoader that should be used in converting values into Java Objects.
     * @param name        the name of this cache manager
     * @throws NullPointerException if classLoader or name is null.
     */
    public RICacheManager(String name, ClassLoader classLoader) {
        super(name, classLoader);
        status = Status.UNINITIALISED;
        if (classLoader == null) {
            throw new NullPointerException("No classLoader specified");
        }
        if (name == null) {
            throw new NullPointerException("No name specified");
        }
        status = Status.STARTED;
    }

    /**
     * Returns the status of this CacheManager.
     * <p/>
     *
     * @return one of {@link javax.cache.Status}
     */
    @Override
    public Status getStatus() {
        return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> CacheBuilder<K, V> createCacheBuilder(String cacheName) {

        if (caches.get(cacheName) != null) {
            throw new CacheException("Cache " + cacheName + " already exists");
        }

        //TODO: where did these naming constraints come from?
        if (cacheName == null) {
            throw new NullPointerException("A cache name must must not be null.");
        }
        Pattern searchPattern = Pattern.compile("\\S+");
        Matcher matcher = searchPattern.matcher(cacheName);
        if (!matcher.find()) {
            throw new IllegalArgumentException("A cache name must contain one or more non-whitespace characters");
        }

        return new RICacheBuilder<K, V>(cacheName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        if (status != Status.STARTED) {
            throw new IllegalStateException();
        }
        synchronized (caches) {
            /*
             * Can't really verify that the K/V cast is safe but it is required by the API, using a 
             * local variable for the cast to allow for a minimal scoping of @SuppressWarnings 
             */
            @SuppressWarnings("unchecked")
            final Cache<K, V> cache = (Cache<K, V>) caches.get(cacheName);
            return cache;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Cache<?, ?>> getCaches() {
        synchronized (caches) {
            HashSet<Cache<?, ?>> set = new HashSet<Cache<?, ?>>();
            for (Cache<?, ?> cache : caches.values()) {
                set.add(cache);
            }
            return Collections.unmodifiableSet(set);
        }
    }

    private void addCacheInternal(Cache<?, ?> cache) {
        synchronized (caches) {
            if (caches.get(cache.getName()) != null) {
                throw new CacheException("Cache " + cache.getName() + " already exists");
            }
            caches.put(cache.getName(), cache);
        }
        cache.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeCache(String cacheName) {
        if (status != Status.STARTED) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException();
        }
        Cache<?, ?> oldCache;
        synchronized (caches) {
            oldCache = caches.remove(cacheName);
        }
        if (oldCache != null) {
            oldCache.stop();
        }

        return oldCache != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserTransaction getUserTransaction() {
        throw new UnsupportedOperationException("Transactions are not supported.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(OptionalFeature optionalFeature) {
        return Caching.isSupported(optionalFeature);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (status != Status.STARTED) {
            throw new IllegalStateException();
        }
        super.shutdown();
        ArrayList<Cache<?, ?>> cacheList;
        synchronized (caches) {
            cacheList = new ArrayList<Cache<?, ?>>(caches.values());
            caches.clear();
        }
        for (Cache<?, ?> cache : cacheList) {
            try {
                cache.stop();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error stopping cache: " + cache);
            }
        }
        status = Status.STOPPED;
    }

    @Override
    public <T> T unwrap(java.lang.Class<T> cls) {
        if (cls.isAssignableFrom(this.getClass())) {
            return cls.cast(this);
        }

        throw new IllegalArgumentException("Unwapping to " + cls + " is not a supported by this implementation");
    }

    /**
     * Obtain the logger.
     *
     * @return the logger.
     */
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * RI implementation of {@link CacheBuilder}
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    private class RICacheBuilder<K, V> extends DelegatingCacheBuilder<K, V> {
        public RICacheBuilder(String cacheName) {
            super(new RICache.Builder<K, V>(cacheName, getName(), getImmutableClasses(), getClassLoader()));
        }

        @Override
        public Cache<K, V> build() {
            Cache<K, V>  cache = super.build();
            addCacheInternal(cache);
            return cache;
        }
    }
}
