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
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheManager;
import javax.cache.CacheManagerFactory;
import javax.cache.OptionalFeature;
import javax.cache.Status;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.NotificationScope;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The reference implementation for JSR107.
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 */
public class RICacheManager implements CacheManager {

    private static final Logger LOGGER = Logger.getLogger("javax.cache");
    private final HashMap<String, Cache> caches = new HashMap<String, Cache>();
    private final String name;
    private final ClassLoader classLoader;
    private volatile Status status;

    /**
     * Constructs a new RICacheManager with the specified name.
     *
     * @param classLoader the ClassLoader that should be used in converting values into Java Objects.
     * @param name the name of this cache manager
     * @throws NullPointerException if classLoader or name is null.
     */
    public RICacheManager(String name, ClassLoader classLoader) {
        status = Status.UNINITIALISED;
        if (classLoader == null) {
            throw new NullPointerException("No classLoader specified");
        }
        if (name == null) {
            throw new NullPointerException("No name specified");
        }
        this.classLoader = classLoader;
        this.name = name;
        status = Status.STARTED;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The name returned will be that passed in to the constructor {@link #RICacheManager(String, ClassLoader)}
     */
    @Override
    public String getName() {
        return name;
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
        return new RICacheBuilder<K, V>(cacheName, classLoader, name);
    }

    /**
     * Will return an RI implementation.
     *
     * {@inheritDoc}
     */
    @Override
    public CacheConfiguration createCacheConfiguration() {
        return new RICacheConfiguration.Builder().build();
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
            return caches.get(cacheName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Cache> getCaches() {
        synchronized (caches) {
            return Collections.unmodifiableSet(new HashSet<Cache>(caches.values()));
        }
    }

    private void addCacheInternal(Cache<?, ?> cache) throws CacheException {
        Cache oldCache;
        synchronized (caches) {
            oldCache = caches.put(cache.getName(), cache);
        }
        cache.start();
        if (oldCache != null) {
            oldCache.stop();
        }
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
        Cache oldCache;
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
    public Object getUserTransaction() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(OptionalFeature optionalFeature) {
        return CacheManagerFactory.isSupported(optionalFeature);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (status != Status.STARTED) {
            throw new IllegalStateException();
        }
        status = Status.STOPPING;
        ArrayList<Cache> cacheList;
        synchronized (caches) {
            cacheList = new ArrayList<Cache>(caches.values());
            caches.clear();
        }
        for (Cache cache : cacheList) {
            try {
                cache.stop();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error stopping cache: " + cache);
            }
        }
        status = Status.STOPPED;
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
    private class RICacheBuilder<K, V> implements CacheBuilder<K, V> {
        private final RICache.Builder<K, V> cacheBuilder;

        public RICacheBuilder(String cacheName, ClassLoader classLoader, String managerName) {
            cacheBuilder = new RICache.Builder<K, V>(cacheName, classLoader, managerName);
        }

        @Override
        public Cache<K, V> build() {
            Cache<K, V> cache = cacheBuilder.build();
            addCacheInternal(cache);
            return cache;
        }

        @Override
        public CacheBuilder<K, V> setCacheConfiguration(CacheConfiguration configuration) {
            cacheBuilder.setCacheConfiguration(configuration);
            return this;
        }

        @Override
        public CacheBuilder<K, V> setCacheLoader(CacheLoader<K, V> cacheLoader) {
            cacheBuilder.setCacheLoader(cacheLoader);
            return this;
        }

        @Override
        public CacheBuilder<K, V> registerCacheEntryListener(CacheEntryListener<K, V> listener, NotificationScope scope, boolean synchronous) {
            cacheBuilder.registerCacheEntryListener(listener, scope, synchronous);
            return this;
        }
    }
}
