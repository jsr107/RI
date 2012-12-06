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
package org.jsr107.ri.annotations;

import java.util.Collections;
import java.util.Set;

import javax.cache.CacheConfiguration;
import javax.cache.CacheEntryExpiryPolicy;
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;
import javax.cache.event.CacheEntryListenerRegistration;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;

/**
 * Provides a {@link CacheConfiguration} that always returns standard default
 * values.
 * 
 * @param <K> the type of the key values
 * @param <V> the type of the values
 * 
 * @author Brian Oliver
 */
public class DefaultCacheConfiguration<K, V> implements CacheConfiguration<K, V> {

    /**
     * {@inheritDoc}
     */    
    @Override
    public Iterable<CacheEntryListenerRegistration<? super K, ? super V>> getCacheEntryListenerRegistrations() {
        return ((Set<CacheEntryListenerRegistration<? super K, ? super V>>)Collections.EMPTY_SET);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheLoader<K, ? extends V> getCacheLoader() {
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public CacheWriter<? super K, ? super V> getCacheWriter() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public CacheEntryExpiryPolicy<? super K, ? super V> getCacheEntryExpiryPolicy() {
        return new CacheEntryExpiryPolicy.Default<K, V>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IsolationLevel getTransactionIsolationLevel() {
        return IsolationLevel.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mode getTransactionMode() {
        return Mode.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadThrough() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStatisticsEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStoreByValue() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransactionEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteThrough() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatisticsEnabled(boolean enableStatistics) {
        throw new UnsupportedOperationException("statistics can't be enabled for this configuration");
    }
}
