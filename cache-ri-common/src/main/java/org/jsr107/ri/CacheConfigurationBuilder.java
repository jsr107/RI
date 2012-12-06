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
import javax.cache.CacheEntryExpiryPolicy;
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;

/**
 * A {@link CacheConfigurationBuilder} is responsible for building {@link CacheConfiguration}s.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <B> the {@link CacheConfigurationBuilder} that is returned from fluent-style method calls
 * 
 * @author Brian Oliver
 * 
 * @since 1.0
 */
public interface CacheConfigurationBuilder<K, V, B extends CacheConfigurationBuilder<K, V, ? extends B>> {

    /**
     * Builds a {@link CacheConfiguration} based on the state of the 
     * {@link CacheConfigurationBuilder}.
     * <p/>
     * Important: The returned {@link CacheConfiguration} is designed to be used 
     * by a single Cache.   Should additional {@link CacheConfiguration}s
     * be required, the developer should call {@link #build()} again to create
     * new instances. 
     * 
     * @return a {@link CacheConfiguration}
     */
    CacheConfiguration<K, V> build();

    /**
     * Sets if a Cache should operate in "read-through" mode.
     * 
     * @param isReadThrough will the built {@link CacheConfiguration} establish
     *                         "read-through" mode
     * @return the {@link CacheConfigurationBuilder}
     * @see CacheConfiguration#isReadThrough()
     */
    B setReadThrough(boolean isReadThrough);
    
    /**
     * Sets if a Cache should operate in "write-through" mode.
     * 
     * @param isWriteThrough will the built {@link CacheConfiguration} establish 
     *                          "write-through" mode
     * @return the {@link CacheConfigurationBuilder}
     * 
     * @see CacheConfiguration#isWriteThrough()
     */
    B setWriteThrough(boolean isWriteThrough);

    /**
     * Sets the {@link CacheLoader}.  
     * <p/>
     * When set to a non-null value, a Cache configured with this
     * a {@link CacheConfiguration} from this builder will support read-through
     * semantics.  When set to a null value, "read-through" won't be provided.
     *
     * @param cacheLoader the {@link CacheLoader}
     * @return the {@link CacheConfigurationBuilder}
     */
    B setCacheLoader(CacheLoader<K, ? extends V> cacheLoader);

    /**
     * Sets the {@link CacheWriter}.
     * <p/>
     * When set to a non-null value, a Cache configured with this
     * a {@link CacheConfiguration} from this builder will support write-through
     * semantics.  When set to a null value, "write-through" won't be provided.
     * 
     * @param cacheWriter the {@link CacheWriter}
     * @return the {@link CacheConfigurationBuilder}
     */
    B setCacheWriter(CacheWriter<? super K, ? super V> cacheWriter);

    /**
     * Adds the definition of a {@link CacheEntryListener} to be registered
     * with a Cache.
     * 
     * @see Cache#registerCacheEntryListener(CacheEntryListener, boolean, CacheEntryEventFilter, boolean)
     * 
     * @param cacheEntryListener The listener to add
     * @param requireOldValue    is the old value supplied in an event
     * @param cacheEntryFilter   the filter to be applied to events
     * @param synchronous        whether the caller is blocked until the listener invocation completes
     * @return the {@link CacheConfigurationBuilder}
     */
    B addCacheEntryListener(CacheEntryListener<? super K, ? super V> cacheEntryListener,
                            boolean requireOldValue,
                            CacheEntryEventFilter<? super K, ? super V> cacheEntryFilter,
                            boolean synchronous);
    
    /**
     * Sets whether the cache is store-by-value cache.
     *
     * @param storeByValue the value for storeByValue
     * @return the {@link CacheConfigurationBuilder}
     * @see CacheConfiguration#isStoreByValue()
     */
    B setStoreByValue(boolean storeByValue);

    /**
     * Sets whether transaction are enabled for this cache.
     *
     * @param isolationLevel - the isolation level for this cache
     * @param mode - the mode (Local or XA) for this cache
     * @return the {@link CacheConfigurationBuilder}
     * @throws IllegalArgumentException if the cache does not support transactions,
     *            or an attempt is made to set the isolation level to {@link IsolationLevel#NONE} or the mode to {@link Mode#NONE}.
     * @see CacheConfiguration#isTransactionEnabled()
     */
    B setTransactions(IsolationLevel isolationLevel, Mode mode);

    /**
     * Sets whether statistics gathering is enabled on this cache.
     *
     * @param isStatisticsEnabled true to enable statistics, false to disable
     * @return the {@link CacheConfigurationBuilder}
     * @see CacheConfiguration#setStatisticsEnabled(boolean)
     */
    B setStatisticsEnabled(boolean isStatisticsEnabled);

    /**
     * Sets the {@link CacheEntryExpiryPolicy}.
     * 
     * @param policy the {@link CacheEntryExpiryPolicy}
     * @return the {@link CacheConfigurationBuilder}
     * @throws NullPointerException if the policy is <code>null</code>
     */
    B setCacheEntryExpiryPolicy(CacheEntryExpiryPolicy<? super K, ? super V> policy);
}
