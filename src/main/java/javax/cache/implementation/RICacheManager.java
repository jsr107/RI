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
import java.util.concurrent.ConcurrentHashMap;
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
    private final ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
    private final String name;

    /**
     * Constructs a new RICacheManager with the specified name.
     *
     * @param name the name of this cache manager
     * @throws NullPointerException if name is null.
     */
    public RICacheManager(String name) {
        if (name == null) {
            throw new NullPointerException("No name specified");
        }
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The name returned will be that passed in to the constructor {@link #RICacheManager(String)}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public <K, V> CacheBuilder<K, V> createCacheBuilder(String cacheName) {
        return new RICacheBuilder<K, V>(cacheName);
    }

    /**
     * {@inheritDoc}
     */
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return caches.get(cacheName);
    }

    private void addCache(Cache<?, ?> cache) throws CacheException {
        Cache oldCache = null;
        synchronized (caches) {
            oldCache = caches.put(cache.getCacheName(), cache);
        }
        cache.start();
        if (oldCache != null) {
            oldCache.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeCache(String cacheName) {
        Cache cache = caches.remove(cacheName);
        if (cache != null) {
            cache.stop();
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getUserTransaction() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        for (Cache cache : caches.values()) {
            try {
                cache.stop();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error stopping cache: " + cache);
            }
        }
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

        public RICacheBuilder(String cacheName) {
            cacheBuilder = new RICache.Builder<K, V>(cacheName);
        }

        public Cache<K, V> build() {
            Cache oldCache = null;
            Cache<K, V> cache = cacheBuilder.build();
            addCache(cache);
            return cache;
        }

        public CacheBuilder<K, V> setCacheConfiguration(CacheConfiguration configuration) {
            cacheBuilder.setCacheConfiguration(configuration);
            return this;
        }

        public CacheBuilder<K, V> setCacheLoader(CacheLoader<K, V> cacheLoader) {
            cacheBuilder.setCacheLoader(cacheLoader);
            return this;
        }
    }
}
