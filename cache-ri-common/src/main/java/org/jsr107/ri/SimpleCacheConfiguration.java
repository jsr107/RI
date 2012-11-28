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

import java.util.ArrayList;

import javax.cache.CacheConfiguration;
import javax.cache.CacheEntryExpiryPolicy;
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;
import javax.cache.event.CacheEntryListener;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;

/**
 * A Simple implementation of a {@link CacheConfiguration}.
 * 
 * @param <K> the type of keys maintained the cache
 * @param <V> the type of cached values
 * 
 * @author Brian Oliver
 * @since 1.0
 */
public class SimpleCacheConfiguration<K, V> implements CacheConfiguration<K, V> {

    /**
     * The collection of {@link CacheEntryListener}s to register when building
     * a {@link CacheConfiguration}.
     */
    protected ArrayList<CacheEntryListener<? super K, ? super V>> cacheEntryListeners;

    /**
     * The {@link CacheLoader} for the built {@link CacheConfiguration}.
     */
    protected CacheLoader<K, ? extends V> cacheLoader;
    
    /**
     * The {@link CacheWriter} for the built {@link CacheConfiguration}.
     */
    protected CacheWriter<? super K, ? super V> cacheWriter;
    
    /**
     * The {@link CacheEntryExpiryPolicy} for the {@link CacheConfiguration}.
     */
    protected CacheEntryExpiryPolicy<? super K, ? super V> cacheEntryExpiryPolicy;
    
    /**
     * A flag indicating if "read-through" mode is required.
     */
    protected boolean isReadThrough;
    
    /**
     * A flag indicating if "write-through" mode is required.
     */
    protected boolean isWriteThrough;
    
    /**
     * A flag indicating if statistics gathering is enabled.
     */
    protected boolean isStatisticsEnabled;

    /**
     * A flag indicating if the cache will be store-by-value or store-by-reference.
     */
    protected boolean isStoreByValue;
    
    /**
     * The transaction {@link IsolationLevel}.
     */
    protected IsolationLevel txnIsolationLevel;

    /**
     * The transaction {@link Mode}.
     */
    protected Mode txnMode;
    
    /**
     * Constructs a {@link SimpleCacheConfiguration}.
     * 
     * @param cacheEntryListeners
     * @param cacheLoader
     * @param cacheWriter
     * @param cacheEntryExpiryPolicy
     * @param isReadThrough
     * @param isWriteThrough
     * @param isStatisticsEnabled
     * @param storeByValue
     * @param txnIsolationLevel
     * @param txnMode
     */
    public SimpleCacheConfiguration(
            Iterable<CacheEntryListener<? super K, ? super V>> cacheEntryListeners,
            CacheLoader<K, ? extends V> cacheLoader,
            CacheWriter<? super K, ? super V> cacheWriter,
            CacheEntryExpiryPolicy<? super K, ? super V> cacheEntryExpiryPolicy, 
            boolean isReadThrough, boolean isWriteThrough,
            boolean isStatisticsEnabled, boolean isStoreByValue,
            IsolationLevel txnIsolationLevel, Mode txnMode) {
        
        this.cacheEntryListeners = new ArrayList<CacheEntryListener<? super K, ? super V>>();
        for (CacheEntryListener<? super K, ? super V> listener : cacheEntryListeners)
        {
            this.cacheEntryListeners.add(listener);
        }
        
        this.cacheLoader = cacheLoader;
        this.cacheWriter = cacheWriter;
        
        this.cacheEntryExpiryPolicy = cacheEntryExpiryPolicy;
        
        this.isReadThrough = isReadThrough;
        this.isWriteThrough = isWriteThrough;
        
        this.isStatisticsEnabled = isStatisticsEnabled;
        
        this.isStoreByValue = isStoreByValue;
        
        this.txnIsolationLevel = txnIsolationLevel;
        this.txnMode = txnMode;
    }
    
    /**
     * A copy-constructor for a {@link SimpleCacheConfiguration}.
     * 
     * @param configuration  the {@link CacheConfiguration} from which to copy
     */
    public SimpleCacheConfiguration(CacheConfiguration<K, V> configuration) {
        this(configuration.getCacheEntryListeners(), 
             configuration.getCacheLoader(), configuration.getCacheWriter(), 
             configuration.getCacheEntryExpiryPolicy(),
             configuration.isReadThrough(), configuration.isWriteThrough(),
             configuration.isStatisticsEnabled(), configuration.isStoreByValue(),
             configuration.getTransactionIsolationLevel(), configuration.getTransactionMode());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<CacheEntryListener<? super K, ? super V>> getCacheEntryListeners() {
        return this.cacheEntryListeners;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public CacheLoader<K, ? extends V> getCacheLoader() {
        return this.cacheLoader;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public CacheWriter<? super K, ? super V> getCacheWriter() {
        return this.cacheWriter;
    }

    /**
     * {@inheritDoc}
     */
    public CacheEntryExpiryPolicy<? super K, ? super V> getCacheEntryExpiryPolicy() {
        return this.cacheEntryExpiryPolicy;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IsolationLevel getTransactionIsolationLevel() {
        return this.txnIsolationLevel;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Mode getTransactionMode() {
        return this.txnMode;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadThrough() {
        return this.isReadThrough;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteThrough() {
        return this.isWriteThrough;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStoreByValue() {
        return this.isStoreByValue;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStatisticsEnabled() {
        return this.isStatisticsEnabled;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransactionEnabled() {
        return this.txnMode != Mode.NONE;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatisticsEnabled(boolean isStatisticsEnabled) {
        this.isStatisticsEnabled = isStatisticsEnabled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((cacheEntryListeners == null) ? 0 : cacheEntryListeners
                        .hashCode());
        result = prime * result
                + ((cacheLoader == null) ? 0 : cacheLoader.hashCode());
        result = prime * result
                + ((cacheWriter == null) ? 0 : cacheWriter.hashCode());
        result = prime * result
                + ((cacheEntryExpiryPolicy == null) ? 0 : cacheEntryExpiryPolicy.hashCode());
        result = prime * result + (isReadThrough ? 1231 : 1237);
        result = prime * result + (isStatisticsEnabled ? 1231 : 1237);
        result = prime * result + (isStoreByValue ? 1231 : 1237);
        result = prime * result + (isWriteThrough ? 1231 : 1237);
        result = prime
                * result
                + ((txnIsolationLevel == null) ? 0 : txnIsolationLevel
                        .hashCode());
        result = prime * result + ((txnMode == null) ? 0 : txnMode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SimpleCacheConfiguration)) {
            return false;
        }
        SimpleCacheConfiguration other = (SimpleCacheConfiguration) obj;
        if (cacheEntryListeners == null) {
            if (other.cacheEntryListeners != null) {
                return false;
            }
        } else if (!cacheEntryListeners.equals(other.cacheEntryListeners)) {
            return false;
        }
        if (cacheLoader == null) {
            if (other.cacheLoader != null) {
                return false;
            }
        } else if (!cacheLoader.equals(other.cacheLoader)) {
            return false;
        }
        if (cacheWriter == null) {
            if (other.cacheWriter != null) {
                return false;
            }
        } else if (!cacheWriter.equals(other.cacheWriter)) {
            return false;
        }
        if (cacheEntryExpiryPolicy == null) {
            if (other.cacheEntryExpiryPolicy != null) {
                return false;
            }
        } else if (!cacheEntryExpiryPolicy.equals(other.cacheEntryExpiryPolicy)) {
            return false;
        }
        if (isReadThrough != other.isReadThrough) {
            return false;
        }
        if (isStatisticsEnabled != other.isStatisticsEnabled) {
            return false;
        }
        if (isStoreByValue != other.isStoreByValue) {
            return false;
        }
        if (isWriteThrough != other.isWriteThrough) {
            return false;
        }
        if (txnIsolationLevel != other.txnIsolationLevel) {
            return false;
        }
        if (txnMode != other.txnMode) {
            return false;
        }
        return true;
    }
}
