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

import javax.cache.CacheConfiguration;
import javax.cache.InvalidConfigurationException;

/**
 * A simple implementation of a {@link CacheConfigurationBuilder} for 
 * {@link SimpleCacheConfiguration}s.
 * 
 * @param <K> the type of keys maintained the cache
 * @param <V> the type of cached values
 * 
 * @author Brian Oliver
 */
public class SimpleCacheConfigurationBuilder<K, V> extends 
    AbstractCacheConfigurationBuilder<K, V, SimpleCacheConfigurationBuilder<K, V>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheConfiguration<K, V> build() {
        
        //ensure we can produce a valid configuration
        if (isReadThrough && cacheLoader == null) {
            throw new InvalidConfigurationException("null cacheLoader when read-through has been configured");
        } 
        
        if (isWriteThrough && cacheWriter == null) {
            throw new InvalidConfigurationException("null cacheWriter when write-through has been configured");
        }
        
        return new SimpleCacheConfiguration<K, V>(
                cacheEntryListeners, 
                cacheLoader, cacheWriter, 
                cacheEntryExpiryPolicy,
                isReadThrough, isWriteThrough, 
                isStatisticsEnabled, 
                storeByValue, 
                txnIsolationLevel, txnMode);
    }
}
