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
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.NotificationScope;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;

/**
 * Class to help implementers
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Yannis Cosmadopoulos
 */
public class DelegatingCacheBuilder<K, V> implements CacheBuilder<K, V> {
    private final AbstractCache.Builder<K, V> cacheBuilder;

    /**
     * Constructor
     * @param cacheBuilder
     */
    public DelegatingCacheBuilder(AbstractCache.Builder<K, V> cacheBuilder) {
        this.cacheBuilder = cacheBuilder;
    }

    @Override
    public Cache<K, V> build() {
        return cacheBuilder.build();
    }

    @Override
    public CacheBuilder<K, V> setCacheLoader(CacheLoader<K, ? extends V> cacheLoader) {
        cacheBuilder.setCacheLoader(cacheLoader);
        return this;
    }

    @Override
    public CacheBuilder<K, V> setCacheWriter(CacheWriter<? super K, ? super V> cacheWriter) {
        cacheBuilder.setCacheWriter(cacheWriter);
        return this;
    }

    @Override
    public CacheBuilder<K, V> registerCacheEntryListener(CacheEntryListener<K, V> listener, NotificationScope scope, boolean synchronous) {
        cacheBuilder.registerCacheEntryListener(listener, scope, synchronous);
        return this;
    }

    @Override
    public CacheBuilder<K, V> setStoreByValue(boolean storeByValue) {
        cacheBuilder.setStoreByValue(storeByValue);
        return this;
    }

    @Override
    public CacheBuilder<K, V> setTransactionEnabled(IsolationLevel isolationLevel, Mode mode) {
        cacheBuilder.setTransactionEnabled(isolationLevel, mode);
        return this;
    }

    @Override
    public CacheBuilder<K, V> setStatisticsEnabled(boolean enableStatistics) {
        cacheBuilder.setStatisticsEnabled(enableStatistics);
        return this;
    }

    @Override
    public CacheBuilder<K, V> setReadThrough(boolean readThrough) {
        cacheBuilder.setReadThrough(readThrough);
        return this;
    }

    @Override
    public CacheBuilder<K, V> setWriteThrough(boolean writeThrough) {
        cacheBuilder.setWriteThrough(writeThrough);
        return this;
    }

    @Override
    public CacheBuilder<K, V> setExpiry(CacheConfiguration.ExpiryType type, CacheConfiguration.Duration duration) {
        cacheBuilder.setExpiry(type, duration);
        return this;
    }
}
