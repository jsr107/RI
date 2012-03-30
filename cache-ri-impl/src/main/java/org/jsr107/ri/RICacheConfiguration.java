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

package org.jsr107.ri;

import javax.cache.CacheLoader;
import javax.cache.CacheWriter;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;

/**
 * The reference implementation for JSR107.
 * <p/>
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 * @author Yannis Cosmadopoulos
 * @author Greg Luck
 * @since 1.0
 */
public final class RICacheConfiguration<K, V> extends AbstractCacheConfiguration {
    private volatile RICache<K, V> riCache;

    private RICacheConfiguration(boolean readThrough,
                                 boolean writeThrough,
                                 boolean storeByValue,
                                 boolean statisticsEnabled,
                                 IsolationLevel isolationLevel, Mode transactionMode,
                                 Duration[] timeToLive) {
        super(readThrough, writeThrough, storeByValue, statisticsEnabled, isolationLevel, transactionMode, timeToLive);
    }

    /**
     * Set the backing cache to expose more configuration.
     * @param riCache the backing cache.
     */
    void setRiCache(RICache<K, V> riCache) {
        this.riCache = riCache;
    }

    /**
     * Gets the registered {@link javax.cache.CacheLoader}, if any.
     *
     * @return the {@link javax.cache.CacheLoader} or null if none has been set.
     */
    @Override
    public CacheLoader<K, ? extends V> getCacheLoader() {
        return riCache.getCacheLoader();
    }

    /**
     * Gets the registered {@link javax.cache.CacheWriter}, if any.
     *
     * @return
     */
    @Override
    public CacheWriter<? super K, ? super V> getCacheWriter() {
        return riCache.getCacheWriter();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Builds the config
     * @author Yannis Cosmadopoulos
     */
    public static class Builder extends AbstractCacheConfiguration.Builder {

        /**
         * Create a new RICacheConfiguration instance.
         *
         * @return a new RICacheConfiguration instance
         */
        public RICacheConfiguration build() {
            return new RICacheConfiguration(readThrough, writeThrough,
                storeByValue, statisticsEnabled,
                isolationLevel, transactionMode,
                timeToLive);
        }
    }
}
