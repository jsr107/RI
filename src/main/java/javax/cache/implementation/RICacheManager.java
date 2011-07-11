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
import javax.cache.CacheException;
import javax.cache.CacheManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The reference implementation for JSR107.
 * <p/>
 * {@inheritDoc}
 *
 * @author Yannis Cosmadopoulos
 */
public class RICacheManager implements CacheManager {
    private static final Logger LOGGER = Logger.getLogger("javax.cache");
    private final ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();

    /**
     * {@inheritDoc}
     */
    public void addCache(Cache<?, ?> cache) throws CacheException {
        cache.start();
        Cache oldCache = caches.put(cache.getCacheName(), cache);
        if (oldCache != null) {
            oldCache.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return caches.get(cacheName);
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
}
