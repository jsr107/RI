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
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The reference implementation for JSR107.
 * <p/>
 * {@inheritDoc}
 *
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public enum RICacheManager implements CacheManager {
    /**
     * the singleton instance
     */
    instance;

    private static final Logger LOGGER = Logger.getLogger("javax.cache");
    private final HashMap<String, Cache> caches = new HashMap<String, Cache>();

    /**
     * {@inheritDoc}
     */
    public void addCache(String cacheName, Cache<?, ?> cache) throws IllegalStateException, CacheException {
        caches.put(cacheName, cache);
    }

    /**
     * {@inheritDoc}
     */
    public <K, V> Cache<K, V> createCache(String cacheName) throws CacheException {
        Cache<K, V> cache = new RICache.Builder<K, V>().build();
        caches.put(cacheName, cache);
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    public <K, V> Cache<K, V> createCache(String cacheName, CacheConfiguration configuration) throws CacheException {
        Cache<K, V> cache = new RICache.Builder<K, V>().setCacheConfiguration(configuration).build();
        caches.put(cacheName, cache);
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    public boolean cacheExists(String cacheName) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getCacheNames() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public <K, V> Cache<K, V> getCache(String cacheName) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public <K, V> Cache<K, V> getOrCreateCache(String cacheName) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeCache(String cacheName) throws IllegalStateException {
        throw new UnsupportedOperationException();
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
}
