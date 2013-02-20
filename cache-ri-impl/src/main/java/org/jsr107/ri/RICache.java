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

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheMXBean;
import javax.cache.CacheManager;
import javax.cache.CacheStatisticsMXBean;
import javax.cache.CacheWriter;
import javax.cache.Caching;
import javax.cache.Configuration;
import javax.cache.Configuration.Duration;
import javax.cache.ExpiryPolicy;
import javax.cache.Status;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryListenerRegistration;
import javax.cache.event.CacheEntryReadListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.CompletionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The reference implementation for JSR107.
 * <p/>
 * This is meant to act as a proof of concept for the API. It is not threadsafe or high performance and does limit
 * the size of caches or provide eviction. It therefore is not suitable for use in production. Please use a
 * production implementation of the API.
 * <p/>
 * This implementation implements all optional parts of JSR107 except for the Transactions chapter. Transactions support
 * simply uses the JTA API. The JSR107 specification details how JTA should be applied to caches.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * 
 * @author Brian Oliver
 * @author Greg Luck
 * @author Yannis Cosmadopoulos
 */
public final class RICache<K, V> implements Cache<K, V> {

    /**
     * The name of the {@link Cache} as used with in the scope of the 
     * Cache Manager.
     */
    private final String cacheName;
    
    /**
     * The name of the Cache Manager as used with in the scope of the
     * Cache Manager Factory.
     */
    private final String cacheManagerName;
    
    /**
     * The {@link ClassLoader} to use for deserializing classes (when necessary).
     */
    private final ClassLoader classLoader;
    
    /**
     * The {@link Configuration} for the {@link Cache}.
     */
    private final Configuration<K, V> configuration;
    
    /**
     * The {@link RIInternalConverter} for keys.
     */
    private final RIInternalConverter<K> keyConverter;
    
    /**
     * The {@link RIInternalConverter} for values.
     */
    private final RIInternalConverter<V> valueConverter;
    
    /**
     * The {@link RIInternalMap} used to store cache entries, keyed by the 
     * internal representation of a key.
     */
    private final RIInternalMap<Object, RICachedValue> entries;

    /**
     * The {@link javax.cache.ExpiryPolicy} for the Cache.
     */
    private final ExpiryPolicy<? super K, ? super V> expiryPolicy;
    
    /**
     * The {@link CacheEntryListenerRegistration}s for the Cache.
     */
    private final ConcurrentHashMap<CacheEntryListener<? super K, ? super V>,
                                    CacheEntryListenerRegistration<? super K, ? super V>> cacheEntryListenerRegistrations =
        new ConcurrentHashMap<CacheEntryListener<? super K, ? super V>, CacheEntryListenerRegistration<? super K, ? super V>>();

    /**
     * The status of the Cache.
     */
    private volatile Status status;
    
    private final RICacheStatistics statistics;
    private final CacheMXBean mBean;

    /**
     * A {@link LockManager} to control concurrent access to cache entries.
     */
    private final LockManager<K> lockManager = new LockManager<K>();

    /**
     * An {@link ExecutorService} for the purposes of performing asynchronous
     * background work.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    
    /**
     * Constructs a cache.
     *
     * @param cacheName          the cache name
     * @param cacheManagerName   the cache manager name
     * @param classLoader        the class loader
     * @param configuration      the configuration
     */
    RICache(String cacheName, 
            String cacheManagerName,
            ClassLoader classLoader,
            Configuration<K, V> configuration) {
        
        this.cacheName = cacheName;
        this.cacheManagerName = cacheManagerName;
        this.classLoader = classLoader;
        
        //we make a copy of the configuration here so that the provided one
        //may be changed and or used independently for other caches
        this.configuration = new RIConfiguration<K, V>(configuration);
                
        keyConverter = configuration.isStoreByValue() ? 
                            new RISerializingInternalConverter<K>(classLoader) : 
                            new RIReferenceInternalConverter<K>();
        
        valueConverter = configuration.isStoreByValue() ?
                             new RISerializingInternalConverter<V>(classLoader) :
                             new RIReferenceInternalConverter<V>();
        
        this.expiryPolicy = configuration.getExpiryPolicy();
        
        status = Status.UNINITIALISED;
 
        entries = new RISimpleInternalMap<Object, RICachedValue>();
                                             
        statistics = new RICacheStatistics(this);
        
        mBean = new DelegatingCacheMXBean<K, V>(this);

        for (CacheEntryListenerRegistration<? super K, ? super V> r : configuration.getCacheEntryListenerRegistrations()) {
            
            CacheEntryListener<? super K, ? super V> listener = r.getCacheEntryListener();
            CacheEntryEventFilter<? super K, ? super V> filter = r.getCacheEntryFilter();
            boolean oldValueRequired = r.isOldValueRequired();
            boolean synchronous = r.isSynchronous();
            
            RICacheEntryListenerRegistration<K, V> registration = 
                    new RICacheEntryListenerRegistration<K, V>(listener, filter, oldValueRequired, synchronous);
            
            cacheEntryListenerRegistrations.put(listener, registration);
        }
    }

    /**
     * Requests a {@link FutureTask} to be performed.
     * 
     * @param task the {@link FutureTask} to be performed
     */
    protected void submit(FutureTask<?> task) {
        executorService.submit(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return cacheName;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public CacheManager getCacheManager() {
        return Caching.getCacheManager(classLoader, cacheManagerName);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration<K, V> getConfiguration() {
        return configuration;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public V get(K key) {
        checkStatusStarted();
        if (key == null) {
            throw new NullPointerException();
        }
        
        RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
        
        V value = getValue(key, dispatcher);
                        
        dispatcher.dispatch(cacheEntryListenerRegistrations.values());
        
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        checkStatusStarted();
        if (keys.contains(null)) {
            throw new NullPointerException("key");
        }
        // will throw NPE if keys=null
        HashMap<K, V> map = new HashMap<K, V>(keys.size());

        RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
        
        for (K key : keys) {
            V value = getValue(key, dispatcher);
            if (value != null) {
                map.put(key, value);
            }
        }

        dispatcher.dispatch(cacheEntryListenerRegistrations.values());
        
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(K key) {
        checkStatusStarted();
        if (key == null) {
            throw new NullPointerException();
        }
        
        long now = System.currentTimeMillis();
        
        lockManager.lock(key);
        try {
            Object internalKey = keyConverter.toInternal(key);
            RICachedValue cachedValue = entries.get(internalKey);
            
            return cachedValue != null && !cachedValue.isExpiredAt(now);
        } finally {
            lockManager.unLock(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAll(final Iterable<? extends K> keys, final CompletionListener listener) {
        checkStatusStarted();
        if (keys == null) {
            throw new NullPointerException("keys");
        }
        
        final CacheLoader<K, ? extends V> cacheLoader = configuration.getCacheLoader();

        if (cacheLoader == null) {
            if (listener != null) {
                listener.onCompletion();
            }
        } else {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        ArrayList<K> keysToLoad = new ArrayList<K>();
                        for (K key : keys) {
                            if (!containsKey(key)) {
                                keysToLoad.add(key);
                            }
                        }

                        Map<? extends K, ? extends V> loaded = cacheLoader.loadAll(keysToLoad);

                        putAll(loaded);

                        if (listener != null) {
                            listener.onCompletion();
                        }
                    } catch (Exception e) {
                        if (listener != null) {
                            listener.onException(e);
                        }
                    }
                }
            });
        }
    }

    /**
     * Returns statistics MXBean
     */
    public CacheStatisticsMXBean getStatistics() {
        checkStatusStarted();
        if (statisticsEnabled()) {
            return statistics;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(K key, V value) {
        checkStatusStarted();
        if (value == null) {
            throw new NullPointerException("null value specified for key " + key);
        }
        
        long start = statisticsEnabled() ? System.nanoTime() : 0;
        lockManager.lock(key);
        try {
            RIEntry<K, V> entry = new RIEntry<K, V>(key, value);
            writeCacheEntry(entry);

            RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();

            long now = System.currentTimeMillis();

            Object internalKey = keyConverter.toInternal(key);
            Object internalValue = valueConverter.toInternal(value);
            
            RICachedValue cachedValue = entries.get(internalKey);
                  
            boolean isExpired = cachedValue != null && cachedValue.isExpiredAt(now);
            if (cachedValue == null || isExpired) {
                
                if (isExpired) {
                    V expiredValue = valueConverter.fromInternal(cachedValue.get());
                    dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue));
                }

                Duration duration = expiryPolicy.getTTLForCreatedEntry(entry);
                long expiryTime = duration.getAdjustedTime(now);

                cachedValue = new RICachedValue(internalValue, now, expiryTime);

                entries.put(internalKey, cachedValue);
                
                dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value));
            } else {
                Duration duration = expiryPolicy.getTTLForModifiedEntry(entry, new Duration(now, cachedValue.getExpiryTime()));
                long expiryTime = duration.getAdjustedTime(now);
                
                V oldValue = valueConverter.fromInternal(cachedValue.get());
                cachedValue.setInternalValue(internalValue, now);
                cachedValue.setExpiryTime(expiryTime);
                
                dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, oldValue));
            }
            
            dispatcher.dispatch(cacheEntryListenerRegistrations.values());
            
        } finally {
            lockManager.unLock(key);
        }
        if (statisticsEnabled()) {
            statistics.increaseCachePuts(1);
            statistics.addPutTimeNano(System.nanoTime() - start);
        }
    }

    @Override
    public V getAndPut(K key, V value) {
        checkStatusStarted();
        if (value == null) {
            throw new NullPointerException("null value specified for key " + key);
        }

        long start = statisticsEnabled() ? System.nanoTime() : 0;
        long now = System.currentTimeMillis();
        
        V result;
        lockManager.lock(key);
        try {
            RIEntry<K, V> entry = new RIEntry<K, V>(key, value);
            writeCacheEntry(entry);

            RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();

            Object internalKey = keyConverter.toInternal(key);
            Object internalValue = valueConverter.toInternal(value);
            
            RICachedValue cachedValue = entries.get(internalKey);
                    
            boolean isExpired = cachedValue != null && cachedValue.isExpiredAt(now);
            if (cachedValue == null || isExpired) {
                
                if (isExpired) {
                    V expiredValue = valueConverter.fromInternal(cachedValue.get());
                    dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue));
                }

                Duration duration = expiryPolicy.getTTLForCreatedEntry(entry);
                long expiryTime = duration.getAdjustedTime(now);
                
                cachedValue = new RICachedValue(internalValue, now, expiryTime);
                entries.put(internalKey, cachedValue);
                result = null;
                
                dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value));
                                
                //TODO: count the "miss" in the statistics
                
            } else {
                V oldValue = valueConverter.fromInternal(cachedValue.getInternalValue(now));

                dispatcher.addEvent(CacheEntryReadListener.class, new RICacheEntryEvent<K, V>(this, key, oldValue));
                
                Duration duration = expiryPolicy.getTTLForModifiedEntry(entry,
                                                                        new Duration(now, cachedValue.getExpiryTime()));
                long expiryTime = duration.getAdjustedTime(now);
                    
                cachedValue.setInternalValue(internalValue, now);
                cachedValue.setExpiryTime(expiryTime);
                
                result = oldValue;
                
                dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, oldValue));
            }
            
            dispatcher.dispatch(cacheEntryListenerRegistrations.values());
            
        } finally {
            lockManager.unLock(key);
        }
        if (statisticsEnabled()) {
            statistics.increaseCachePuts(1);
            statistics.addPutTimeNano(System.nanoTime() - start);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        checkStatusStarted();
        long start = statisticsEnabled() ? System.nanoTime() : 0;
        
        long now = System.currentTimeMillis();

        if (map.containsKey(null)) {
            throw new NullPointerException("key");
        }

        CacheException exception = null;

        RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();

        try {
            boolean isWriteThrough = configuration.isWriteThrough() && configuration.getCacheWriter() != null;

            //lock all of the keys in the map
            ArrayList<Cache.Entry<? extends K, ? extends V>> entriesToWrite = new ArrayList<Cache.Entry<? extends K, ? extends V>>();
            HashSet<K> keysToPut = new HashSet<K>();
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                K key = entry.getKey();
                V value = entry.getValue();

                if (value == null) {
                    throw new NullPointerException("key " + key + " has a null value");
                }

                lockManager.lock(key);

                keysToPut.add(key);

                if (isWriteThrough) {
                    entriesToWrite.add(new RIEntry<K, V>(key, value));
                }
            }

            //write the entries
            if (isWriteThrough) {
                try {
                    CacheWriter<K, V> writer = (CacheWriter<K, V>)configuration.getCacheWriter();
                    writer.writeAll(entriesToWrite);
                } catch (CacheException e) {
                    exception = e;
                }

                for (Entry entry : entriesToWrite) {
                    keysToPut.remove(entry.getKey());
                }
            }

            //perform the put
            for (K key : keysToPut) {
                V value = map.get(key);

                Object internalKey = keyConverter.toInternal(key);
                Object internalValue = valueConverter.toInternal(value);

                RICachedValue cachedValue = entries.get(internalKey);

                boolean isExpired = cachedValue != null && cachedValue.isExpiredAt(now);
                if (cachedValue == null || isExpired) {

                    if (isExpired) {
                        V expiredValue = valueConverter.fromInternal(cachedValue.get());
                        dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue));
                    }

                    Duration duration = expiryPolicy.getTTLForCreatedEntry(new RIEntry<K, V>(key, value));
                    long expiryTime = duration.getAdjustedTime(now);

                    cachedValue = new RICachedValue(internalValue, now, expiryTime);

                    entries.put(internalKey, cachedValue);

                    dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value));
                } else {
                    Duration duration = expiryPolicy.getTTLForModifiedEntry(new RIEntry<K, V>(key, value),
                            new Duration(now, cachedValue.getExpiryTime()));
                    long expiryTime = duration.getAdjustedTime(now);

                    cachedValue.setInternalValue(internalValue, now);
                    cachedValue.setExpiryTime(expiryTime);

                    V oldValue = valueConverter.fromInternal(cachedValue.get());
                    dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, oldValue));
                }
            }
        } finally {
            //unlock all of the keys
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                K key = entry.getKey();
                V value = entry.getValue();

                lockManager.unLock(key);
            }
        }

        //dispatch events
        dispatcher.dispatch(cacheEntryListenerRegistrations.values());

        if (statisticsEnabled()) {
            statistics.increaseCachePuts(map.size());
            statistics.addPutTimeNano(System.nanoTime() - start);
        }

        if (exception != null) {
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putIfAbsent(K key, V value) {
        checkStatusStarted();
        if (value == null) {
            throw new NullPointerException("null value specified for key " + key);
        }

        long start = statisticsEnabled() ? System.nanoTime() : 0;
        
        long now = System.currentTimeMillis();
        
        boolean result;
        lockManager.lock(key);
        try {
            RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();

            Object internalKey = keyConverter.toInternal(key);
            Object internalValue = valueConverter.toInternal(value);
            
            RICachedValue cachedValue = entries.get(internalKey);
                    
            boolean isExpired = cachedValue != null && cachedValue.isExpiredAt(now);
            if (cachedValue == null || cachedValue.isExpiredAt(now)) {

                RIEntry<K, V> entry = new RIEntry<K, V>(key, value);
                writeCacheEntry(entry);

                if (isExpired) {
                    V expiredValue = valueConverter.fromInternal(cachedValue.get());
                    dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue));
                }

                Duration duration = expiryPolicy.getTTLForCreatedEntry(entry);
                long expiryTime = duration.getAdjustedTime(now);
                
                cachedValue = new RICachedValue(internalValue, now, expiryTime);
                entries.put(internalKey, cachedValue);
                result = true;
                
                dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value));
            } else {
                result = false;
            }
            
            dispatcher.dispatch(cacheEntryListenerRegistrations.values());
            
        } finally {
            lockManager.unLock(key);
        }
        
        //TODO: this is incorrect.  it should only do this if we actually do a put
        if (result && statisticsEnabled()) {
            statistics.increaseCachePuts(1);
            statistics.addPutTimeNano(System.nanoTime() - start);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(K key) {
        checkStatusStarted();
        long start = statisticsEnabled() ? System.nanoTime() : 0;
        
        long now = System.currentTimeMillis();
        
        boolean result;
        lockManager.lock(key);
        try {
            deleteCacheEntry(key);

            Object internalKey = keyConverter.toInternal(key);
            RICachedValue cachedValue = entries.get(internalKey);
            
            if (cachedValue == null) {
                return false;
            } else if (cachedValue.isExpiredAt(now)) {
                result = false;
            } else {
                entries.remove(internalKey);
                V value = valueConverter.fromInternal(cachedValue.get());
                
                RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
                dispatcher.addEvent(CacheEntryRemovedListener.class, new RICacheEntryEvent<K, V>(this, key, value));
                dispatcher.dispatch(cacheEntryListenerRegistrations.values());
                
                result = true;
            }
        } finally {
            lockManager.unLock(key);
        }
        if (result && statisticsEnabled()) {
            statistics.increaseCacheRemovals(1);
            statistics.addRemoveTimeNano(System.nanoTime() - start);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(K key, V oldValue) {
        checkStatusStarted();
        if (oldValue == null) {
            throw new NullPointerException("null oldValue specified for key " + key);
        }
        
        long now = System.currentTimeMillis();
                
        long start = statisticsEnabled() ? System.nanoTime() : 0;
        boolean result;
        lockManager.lock(key);
        try {
            Object internalKey = keyConverter.toInternal(key);
            RICachedValue cachedValue = entries.get(internalKey);
            if (cachedValue == null || cachedValue.isExpiredAt(now)) {
                result = false;
            } else {
                Object internalValue = cachedValue.get();
                Object oldInternalValue = valueConverter.toInternal(oldValue);
                
                if (internalValue.equals(oldInternalValue)) {
                    deleteCacheEntry(key);

                    entries.remove(internalKey);
                    
                    RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
                    dispatcher.addEvent(CacheEntryRemovedListener.class, new RICacheEntryEvent<K, V>(this, key, oldValue));
                    dispatcher.dispatch(cacheEntryListenerRegistrations.values());
                    
                    result = true;
                } else {
                    result = false;
                }
            }
        } finally {
            lockManager.unLock(key);
        }
        if (result && statisticsEnabled()) {
            statistics.increaseCacheRemovals(1);
            statistics.addRemoveTimeNano(System.nanoTime() - start);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndRemove(K key) {
        checkStatusStarted();
        
        long now = System.currentTimeMillis();
        
        V result;
        lockManager.lock(key);
        try {
            deleteCacheEntry(key);

            Object internalKey = keyConverter.toInternal(key);
            RICachedValue cachedValue = entries.get(internalKey);
            if (cachedValue == null || cachedValue.isExpiredAt(now)) {
                result = null;
            } else {
                entries.remove(internalKey);
                result = valueConverter.fromInternal(cachedValue.getInternalValue(now));
                
                CacheEntryEvent<K, V> event = new RICacheEntryEvent<K, V>(this, key, result);
                RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
                dispatcher.addEvent(CacheEntryReadListener.class, event);
                dispatcher.addEvent(CacheEntryRemovedListener.class, event);
                dispatcher.dispatch(cacheEntryListenerRegistrations.values());
            }
        } finally {
            lockManager.unLock(key);
        }
        if (statisticsEnabled()) {
            if (result != null) {
                statistics.increaseCacheHits(1);
                statistics.increaseCacheRemovals(1);
            } else {
                statistics.increaseCacheMisses(1);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        checkStatusStarted();
        if (newValue == null) {
            throw new NullPointerException("null newValue specified for key " + key);
        }

        if (oldValue == null) {
            throw new NullPointerException("null oldValue specified for key " + key);
        }

        long now = System.currentTimeMillis();
        
        boolean result;
        lockManager.lock(key);
        try {
            Object internalKey = keyConverter.toInternal(key);
            RICachedValue cachedValue = entries.get(internalKey);
            if (cachedValue == null || cachedValue.isExpiredAt(now)) {
                result = false;
            } else {
                Object oldInternalValue = valueConverter.toInternal(oldValue);
                
                if (cachedValue.get().equals(oldInternalValue)) {
                    RIEntry<K, V> entry = new RIEntry<K, V>(key, newValue);
                    writeCacheEntry(entry);

                    Duration duration = expiryPolicy.getTTLForModifiedEntry(entry,
                                                                            new Duration(now, cachedValue.getExpiryTime()));
                    long expiryTime = duration.getAdjustedTime(now);
                    
                    Object newInternalValue = valueConverter.toInternal(newValue);
                    cachedValue.setInternalValue(newInternalValue, now);
                    cachedValue.setExpiryTime(expiryTime);
                    
                    RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
                    dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, newValue, oldValue));
                    dispatcher.dispatch(cacheEntryListenerRegistrations.values());
                    
                    result = true;
                } else {
                    result = false;
                }
            }
        } finally {
            lockManager.unLock(key);
        }
        if (result && statisticsEnabled()) {
            statistics.increaseCachePuts(1);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V value) {
        checkStatusStarted();
        if (value == null) {
            throw new NullPointerException("null value specified for key " + key);
        }

        long now = System.currentTimeMillis();

        boolean result;
        lockManager.lock(key);
        try {
            Object internalKey = keyConverter.toInternal(key);
            RICachedValue cachedValue = entries.get(internalKey);
            if (cachedValue == null || cachedValue.isExpiredAt(now)) {
                result = false;
            } else {
                RIEntry<K, V> entry = new RIEntry<K, V>(key, value);
                writeCacheEntry(entry);

                V previousValue = valueConverter.fromInternal(cachedValue.get());
                        
                Duration duration = expiryPolicy.getTTLForModifiedEntry(entry,
                                                                        new Duration(now, cachedValue.getExpiryTime()));
                long expiryTime = duration.getAdjustedTime(now);

                Object internalValue = valueConverter.toInternal(value);
                cachedValue.setInternalValue(internalValue, now);
                cachedValue.setExpiryTime(expiryTime);
                
                RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
                dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, previousValue));
                dispatcher.dispatch(cacheEntryListenerRegistrations.values());
                
                result = true;
            }
        } finally {
            lockManager.unLock(key);
        }
        if (result && statisticsEnabled()) {
            statistics.increaseCachePuts(1);
        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndReplace(K key, V value) {
        checkStatusStarted();
        if (value == null) {
            throw new NullPointerException("null value specified for key " + key);
        }

        long now = System.currentTimeMillis();

        V result;
        lockManager.lock(key);
        try {
            Object internalKey = keyConverter.toInternal(key);
            RICachedValue cachedValue = entries.get(internalKey);
            if (cachedValue == null || cachedValue.isExpiredAt(now)) {
                result = null;
            } else {
                RIEntry<K, V> entry = new RIEntry<K, V>(key, value);
                writeCacheEntry(entry);

                result = valueConverter.fromInternal(cachedValue.getInternalValue(now));
                
                Duration duration = expiryPolicy.getTTLForModifiedEntry(entry,
                                                                        new Duration(now, cachedValue.getExpiryTime()));
                long expiryTime = duration.getAdjustedTime(now);
                
                Object internalValue = valueConverter.toInternal(value);
                cachedValue.setInternalValue(internalValue, now);
                cachedValue.setExpiryTime(expiryTime);

                RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
                dispatcher.addEvent(CacheEntryReadListener.class, new RICacheEntryEvent<K, V>(this, key, result));
                dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, result));
                dispatcher.dispatch(cacheEntryListenerRegistrations.values());
            }
        } finally {
            lockManager.unLock(key);
        }
        if (statisticsEnabled()) {
            if (result != null) {
                statistics.increaseCacheHits(1);
                statistics.increaseCachePuts(1);
            } else {
                statistics.increaseCacheMisses(1);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll(Set<? extends K> keys) {
        checkStatusStarted();
        
        long now = System.currentTimeMillis();

        CacheException exception = null;
        HashSet<K> lockedKeys = new HashSet<K>();

        RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();

        try {
            boolean isWriteThrough = configuration.isWriteThrough() && configuration.getCacheWriter() != null;

            //lock all of the keys
            HashSet<K> keysToDelete = new HashSet<K>();

            for (K key : keys) {
                lockManager.lock(key);

                lockedKeys.add(key);

                if (isWriteThrough) {
                    keysToDelete.add(key);
                }
            }

            //delete the entries
            if (isWriteThrough) {
                try {
                    CacheWriter<K, V> writer = (CacheWriter<K, V>)configuration.getCacheWriter();
                    writer.deleteAll(keysToDelete);
                } catch (CacheException e) {
                    exception = e;
                }
            }

            //remove the deleted keys that were successfully deleted
            for (K key : lockedKeys) {
                if (!keysToDelete.contains(key)) {
                    Object internalKey = keyConverter.toInternal(key);
                    RICachedValue cachedValue = entries.remove(internalKey);

                    V value = valueConverter.fromInternal(cachedValue.get());

                    RICacheEntryEvent<K, V> event = new RICacheEntryEvent<K, V>(this, key, value);

                    if (cachedValue.isExpiredAt(now)) {
                        dispatcher.addEvent(CacheEntryExpiredListener.class, event);
                    } else {
                        dispatcher.addEvent(CacheEntryRemovedListener.class, event);
                    }
                }
            }

        } finally {
            //unlock all of the keys
            for (K key : lockedKeys) {
                lockManager.unLock(key);
            }
        }

        dispatcher.dispatch(cacheEntryListenerRegistrations.values());

        //TODO: this should simply be the number of actual entries removed (not including expired)
        if (statisticsEnabled()) {
            statistics.increaseCacheRemovals(keys.size());
        }

        if (exception != null) {
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() {
        checkStatusStarted();

        //TODO: this is incorrect as the internal map may contain expired entries
        int size = (statisticsEnabled()) ? entries.size() : 0;

        long now = System.currentTimeMillis();

        CacheException exception = null;
        HashSet<K> lockedKeys = new HashSet<K>();

        RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();

        try {
            boolean isWriteThrough = configuration.isWriteThrough() && configuration.getCacheWriter() != null;

            //lock all of the keys
            HashSet<K> keysToDelete = new HashSet<K>();

            Iterator<Map.Entry<Object, RICachedValue>> iterator = entries.iterator();

            while (iterator.hasNext()) {
                Map.Entry<Object, RICachedValue> entry = iterator.next();

                Object internalKey = entry.getKey();
                K key = keyConverter.fromInternal(internalKey);

                lockManager.lock(key);

                lockedKeys.add(key);

                if (isWriteThrough) {
                    keysToDelete.add(key);
                }
            }

            //delete the entries
            if (isWriteThrough) {
                try {
                    CacheWriter<K, V> writer = (CacheWriter<K, V>)configuration.getCacheWriter();
                    writer.deleteAll(keysToDelete);
                } catch (CacheException e) {
                    exception = e;
                }
            }

            //remove the deleted keys that were successfully deleted from the set
            for (K key : lockedKeys) {
                if (!keysToDelete.contains(key)) {
                    Object internalKey = keyConverter.toInternal(key);
                    RICachedValue cachedValue = entries.remove(internalKey);

                    V value = valueConverter.fromInternal(cachedValue.get());

                    RICacheEntryEvent<K, V> event = new RICacheEntryEvent<K, V>(this, key, value);

                    if (cachedValue.isExpiredAt(now)) {
                        dispatcher.addEvent(CacheEntryExpiredListener.class, event);
                    } else {
                        dispatcher.addEvent(CacheEntryRemovedListener.class, event);
                    }
                }
            }

        } finally {
            //unlock all of the keys
            for (K key : lockedKeys) {
                lockManager.unLock(key);
            }
        }

        dispatcher.dispatch(cacheEntryListenerRegistrations.values());

        //TODO: this should simple be the number of actual entries removed
        if (statisticsEnabled()) {
            statistics.increaseCacheRemovals(size);
        }

        if (exception != null) {
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        checkStatusStarted();

        Iterator<Map.Entry<Object, RICachedValue>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Object, RICachedValue> entry = iterator.next();
            Object internalKey = entry.getKey();
            K key = keyConverter.fromInternal(internalKey);

            lockManager.lock(key);
            try {
                iterator.remove();
            } finally {
                lockManager.unLock(key);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean registerCacheEntryListener(CacheEntryListener<? super K, ? super V> listener,
                                              boolean requireOldValue,
                                              CacheEntryEventFilter<? super K, ? super V> filter,
                                              boolean synchronous) {
        if (listener == null) {
            throw new CacheEntryListenerException("A listener may not be null");
        }
        RICacheEntryListenerRegistration<K, V> registration = 
                new RICacheEntryListenerRegistration<K, V>(listener, filter, requireOldValue, synchronous);
        return cacheEntryListenerRegistrations.putIfAbsent(listener, registration) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unregisterCacheEntryListener(CacheEntryListener<?, ?> listener) {
        if (listener == null) {
            return false;
        } else {
            return cacheEntryListenerRegistrations.remove(listener) != null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeEntryProcessor(K key, EntryProcessor<K, V, T> entryProcessor) {
        checkStatusStarted();
        if (key == null) {
            throw new NullPointerException();
        }
        if (key == entryProcessor) {
            throw new NullPointerException();
        }
        
        T result = null;
        lockManager.lock(key);
        try {
            long now = System.currentTimeMillis();
            
            RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();

            Object internalKey = keyConverter.toInternal(key);
            RICachedValue cachedValue = entries.get(internalKey);
            
            EntryProcessorEntry entry = new EntryProcessorEntry(key, cachedValue, now, dispatcher);
            result = entryProcessor.process(entry);

            Duration duration;
            long expiryTime;
            switch(entry.operation) {
            case NONE:
                break;
                
            case CREATE:
                RIEntry<K, V> e = new RIEntry<K, V>(key, entry.value);
                writeCacheEntry(e);

                duration = expiryPolicy.getTTLForCreatedEntry(e);
                expiryTime = duration.getAdjustedTime(now);
                
                cachedValue = new RICachedValue(valueConverter.toInternal(entry.value), now, expiryTime);
                
                if (cachedValue.isExpiredAt(now)) {
                    V previousValue = valueConverter.fromInternal(cachedValue.get());
                    dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, previousValue));
                }
                
                entries.put(internalKey, cachedValue);
                
                dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, entry.value));
                break;
                
            case UPDATE:
                e = new RIEntry<K, V>(key, entry.value);
                writeCacheEntry(e);

                duration = expiryPolicy.getTTLForModifiedEntry(e, new Duration(now, cachedValue.getExpiryTime()));
                expiryTime = duration.getAdjustedTime(now);
                
                V previousValue = valueConverter.fromInternal(cachedValue.get());
                cachedValue.setInternalValue(valueConverter.toInternal(entry.value), now);
                cachedValue.setExpiryTime(expiryTime);
                
                dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, entry.value, previousValue));
                break;
                
            case REMOVE:
                deleteCacheEntry(key);

                previousValue = valueConverter.fromInternal(cachedValue.get());
                entries.remove(internalKey);
               
                dispatcher.addEvent(CacheEntryRemovedListener.class, new RICacheEntryEvent<K, V>(this, key, previousValue));
                break;
                
            default:
                break;
            }
            
            dispatcher.dispatch(cacheEntryListenerRegistrations.values());
            
        } finally {
            lockManager.unLock(key);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Entry<K, V>> iterator() {
        checkStatusStarted();

        long now = System.currentTimeMillis();
        
        return new RIEntryIterator(entries.iterator(), now);
    }

    /**
     * @return the managemtn bean
     */
    public CacheMXBean getMBean() {
        return mBean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        status = Status.STARTED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new CacheException(e);
        }
        
        entries.clear();
        status = Status.STOPPED;
    }

    private void checkStatusStarted() {
        if (!status.equals(Status.STARTED)) {
            throw new IllegalStateException("The cache status is not STARTED");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public <T> T unwrap(java.lang.Class<T> cls) {
        if (cls.isAssignableFrom(this.getClass())) {
            return cls.cast(this);
        }
        
        throw new IllegalArgumentException("Unwrapping to " + cls + " is not a supported by this implementation");
    }

    private boolean statisticsEnabled() {
        return getConfiguration().isStatisticsEnabled();
    }

    /**
     * Writes the Cache Entry to the configured CacheWriter.  Does nothing if
     * write-through is not configured.
     *
     * @param entry the Cache Entry to write
     */
    private void writeCacheEntry(RIEntry<K, V> entry) {
        if (configuration.isWriteThrough()) {
            configuration.getCacheWriter().write(entry);
        }
    }

    /**
     * Deletes the Cache Entry using the configued CacheWriter.  Does nothing
     * if write-through is not configued.
     * @param key
     */
    private void deleteCacheEntry(K key) {
        if (configuration.isWriteThrough()) {
            configuration.getCacheWriter().delete(key);
        }
    }

    /**
     * Gets the value for the specified key from the underlying cache, including
     * attempting to load it if a CacheLoader is configured (with read-through).
     * <p/>
     * Any events that need to be raised are added to the specified dispatcher.
     *
     * @param key the key of the entry to get from the cache
     * @param dispatcher the dispatcher for events
     * @return the value loaded
     */
    private V getValue(K key, RICacheEventEventDispatcher<K, V> dispatcher) {
        long now = System.currentTimeMillis();
        
        Object internalKey = keyConverter.toInternal(key);
        RICachedValue cachedValue = null;
        V value = null;
        lockManager.lock(key);
        try {
            cachedValue = entries.get(internalKey);
                
            boolean isExpired = cachedValue != null && cachedValue.isExpiredAt(now);

            if (cachedValue == null || isExpired) {

                V expiredValue = isExpired ? valueConverter.fromInternal(cachedValue.get()) : null;

                if (isExpired) {
                    dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue));
                }

                if (statisticsEnabled()) {
                    statistics.increaseCacheMisses(1);
                }
                
                CacheLoader<K, ? extends V> cacheLoader = configuration.getCacheLoader();
                
                if (cacheLoader == null) {
                    return null;
                } 
                
                Entry<K, ? extends V> entry = cacheLoader.load(key);
                
                if (entry == null) {
                    return null;
                } 
                
                value = entry.getValue();
                
                Duration duration = expiryPolicy.getTTLForCreatedEntry(new RIEntry<K, V>(key, value));
                long expiryTime = duration.getAdjustedTime(now);
                
                Object internalValue = valueConverter.toInternal(value);
                cachedValue = new RICachedValue(internalValue, now, expiryTime);
                    
                if (cachedValue.isExpiredAt(now)) {
                    return null;
                } else {

                    
                    entries.put(internalKey, cachedValue);
                    
                    dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value));
                    
                    dispatcher.addEvent(CacheEntryReadListener.class, new RICacheEntryEvent<K, V>(this, key, value));
                }
            } else {
                value = valueConverter.fromInternal(cachedValue.getInternalValue(now));
                RIEntry<K, V> entry = new RIEntry<K, V>(key, value);
                
                Duration duration = expiryPolicy.getTTLForAccessedEntry(entry, new Duration(now, cachedValue.getExpiryTime()));
                long expiryTime = duration.getAdjustedTime(now);
                cachedValue.setExpiryTime(expiryTime);
                
                dispatcher.addEvent(CacheEntryReadListener.class, new RICacheEntryEvent<K, V>(this, key, value));
                
                if (statisticsEnabled()) {
                    statistics.increaseCacheHits(1);
                }
            }
            
        } finally {
            lockManager.unLock(key);
        }
        
        return value;
    }

    /**
     * Returns the size of the cache.
     *
     * @return the size in entries of the cache
     */
    long getSize() {
        return entries.size();
    }
    
    /**
     * {@inheritDoc}
     */
    private static class RIEntry<K, V> implements Entry<K, V> {
        private final K key;
        private final V value;

        public RIEntry(K key, V value) {
            if (key == null) {
                throw new NullPointerException("key");
            }
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RIEntry<?, ?> e2 = (RIEntry<?, ?>) o;

            return this.getKey().equals(e2.getKey()) &&
                    this.getValue().equals(e2.getValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return getKey().hashCode() ^ getValue().hashCode();
        }
    }

    /**
     * An {@link Iterator} over Cache {@link Entry}s that lazily converts
     * from internal value representation to natural value representation on 
     * demand.
     */
    private final class RIEntryIterator implements Iterator<Entry<K, V>> {

        /**
         * The {@link Iterator} over the internal entries.
         */
        private final Iterator<Map.Entry<Object, RICachedValue>> iterator;

        /**
         * The next available non-expired cache entry to return.
         */
        private RIEntry<K, V> nextEntry;

        /**
         * The last returned cache entry (so we can allow for removal)
         */
        private RIEntry<K, V> lastEntry;

        /**
         * The time the iteration commenced.  We use this to determine what
         * Cache Entries in the underlying iterator are expired.
         */
        private long now;
        
        /**
         * Constructs an {@link RIEntryIterator}.
         * 
         * @param iterator the {@link Iterator} over the internal entries
         * @param now      the time the iterator will use to test for expiry
         */
        private RIEntryIterator(Iterator<Map.Entry<Object, RICachedValue>> iterator, long now) {
            this.iterator = iterator;
            this.nextEntry = null;
            this.lastEntry = null;
            this.now = now;
        }

        /**
         * Fetches the next available, non-expired entry from the underlying 
         * iterator.
         */
        private void fetch() {

            while (nextEntry == null && iterator.hasNext()) {

                Map.Entry<Object, RICachedValue> entry = iterator.next();
                RICachedValue cachedValue = entry.getValue();

                K key = (K)RICache.this.keyConverter.fromInternal(entry.getKey());
                lockManager.lock(key);
                try {
                    if (!cachedValue.isExpiredAt(now)) {
                        V value = (V)RICache.this.valueConverter.fromInternal(cachedValue.getInternalValue(now));
                        nextEntry = new RIEntry<K, V>(key, value);

                        Duration duration = expiryPolicy.getTTLForAccessedEntry(nextEntry, new Duration(now, cachedValue.getExpiryTime()));
                        long expiryTime = duration.getAdjustedTime(now);
                        cachedValue.setExpiryTime(expiryTime);
                    }
                } finally {
                    lockManager.unLock(key);
                }
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            if (nextEntry == null) {
                fetch();
            }
            return nextEntry != null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry<K, V> next() {
            if (hasNext()) {
                //remember the lastEntry (so that we call allow for removal)
                lastEntry = nextEntry;

                //raise "read" event
                RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
                dispatcher.addEvent(CacheEntryReadListener.class,
                                    new RICacheEntryEvent<K, V>(RICache.this, lastEntry.getKey(), lastEntry.getValue()));
                dispatcher.dispatch(cacheEntryListenerRegistrations.values());

                //reset nextEntry to force fetching the next available entry
                nextEntry = null;

                return lastEntry;
            } else {
                throw new NoSuchElementException();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {

            if (lastEntry == null) {
                throw new IllegalStateException("Must progress to the next entry to remove");
            } else {
                lockManager.lock(lastEntry.getKey());
                try {
                    deleteCacheEntry(lastEntry.getKey());

                    //NOTE: there is the possibility here that the entry the application retrieved
                    //may have been replaced / expired or already removed since it retrieved it.

                    //we simply don't care here as multiple-threads are ok to remove and see
                    //such side-effects
                    iterator.remove();

                    //raise "remove" event
                    RICacheEventEventDispatcher<K, V> dispatcher = new RICacheEventEventDispatcher<K, V>();
                    dispatcher.addEvent(CacheEntryRemovedListener.class,
                                        new RICacheEntryEvent<K, V>(RICache.this, lastEntry.getKey(), lastEntry.getValue()));
                    dispatcher.dispatch(cacheEntryListenerRegistrations.values());

                } finally {
                    lockManager.unLock(lastEntry.getKey());

                    //reset lastEntry (we can't attempt to remove it again)
                    lastEntry = null;
                }
            }
        }
    }

    /**
     * Callable used for cache loader.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     */
    private static class RICacheLoaderLoadAllCallable<K, V> implements Callable<Map<K, ? extends V>> {
        private final RICache<K, V> cache;
        private final CacheLoader<K, ? extends V> cacheLoader;
        private final Collection<? extends K> keys;

        RICacheLoaderLoadAllCallable(RICache<K, V> cache, CacheLoader<K, ? extends V> cacheLoader, Collection<? extends K> keys) {
            this.cache = cache;
            this.cacheLoader = cacheLoader;
            this.keys = keys;
        }

        @Override
        public Map<K, ? extends V> call() throws Exception {
            ArrayList<K> keysNotInStore = new ArrayList<K>();
            for (K key : keys) {
                if (!cache.containsKey(key)) {
                    keysNotInStore.add(key);
                }
            }
            Map<K, ? extends V> value = cacheLoader.loadAll(keysNotInStore);
            cache.putAll(value);
            return value;
        }
    }

    /**
     * A mechanism to manage locks for a collection of objects.
     * 
     * @param <K> the type of the object to be locked
     */
    private static final class LockManager<K> {
        private final ConcurrentHashMap<K, ReentrantLock> locks = new ConcurrentHashMap<K, ReentrantLock>();
        private final LockFactory lockFactory = new LockFactory();

        private LockManager() {
        }

        /**
         * Lock the object
         * @param key the key
         */
        private void lock(K key) {
            ReentrantLock lock = lockFactory.getLock();

            while (true) {
                ReentrantLock oldLock = locks.putIfAbsent(key, lock);
                if (oldLock == null) {
                    return;
                }
                // there was a lock
                oldLock.lock();
                // now we have it. Because of possibility that someone had it for remove,
                // we don't re-use directly
                lockFactory.release(oldLock);
            }
        }

        /**
         * Unlock the object
         * @param key the object
         */
        private void unLock(K key) {
            ReentrantLock lock = locks.remove(key);
            lockFactory.release(lock);
        }

        /**
         * A factory for {@link ReentrantLock}s.
         */
        private static final class LockFactory {
            private static final int CAPACITY = 100;
            private static final ArrayList<ReentrantLock> LOCKS = new ArrayList<ReentrantLock>(CAPACITY);

            private LockFactory() {
            }

            private ReentrantLock getLock() {
                ReentrantLock qLock = null;
                synchronized (LOCKS) {
                    if (!LOCKS.isEmpty()) {
                        qLock = LOCKS.remove(0);
                    }
                }

                ReentrantLock lock = qLock != null ? qLock : new ReentrantLock();
                lock.lock();
                return lock;
            }

            private void release(ReentrantLock lock) {
                lock.unlock();
                synchronized (LOCKS) {
                    if (LOCKS.size() <= CAPACITY) {
                        LOCKS.add(lock);
                    }
                }
            }
        }
    }
    
    /**
     * The operation to perform on a {@link RICachedValue} as a result of
     * actions performed on a {@link MutableEntry}.
     */
    private enum MutableEntryOperation {
        /**
         * Don't perform any operations on the {@link RICachedValue}.
         */
        NONE,
        
        /**
         * Create a new {@link RICachedValue}.
         */
        CREATE,
        
        /**
         * Remove the {@link RICachedValue} (and thus the Cache Entry).
         */
        REMOVE,
        
        /**
         * Update the {@link RICachedValue}.
         */
        UPDATE;
    }
    
    /**
     * A {@link MutableEntry} that is used by {@link EntryProcessor}s.
     */
    private class EntryProcessorEntry implements MutableEntry<K, V> {
        /**
         * The key of the {@link MutableEntry}.
         */
        private final K key;
        
        /**
         * The {@link RICachedValue} for the {@link MutableEntry}.
         */
        private final RICachedValue cachedValue;
        
        /**
         * The new value for the {@link MutableEntry}.
         */
        private V value;
        
        /**
         * The {@link MutableEntryOperation} to be performed on the {@link MutableEntry}.
         */
        private MutableEntryOperation operation; 
        
        /**
         * The time (since the Epoc) when the MutableEntry was created.
         */
        private long now;
        
        /**
         * The dispatcher to use for capturing events to eventually dispatch.
         */
        private RICacheEventEventDispatcher<K, V> dispatcher;
        
        /**
         * Construct a {@link MutableEntry}
         * 
         * @param key         the key for the {@link MutableEntry}
         * @param cachedValue the {@link RICachedValue} of the {@link MutableEntry}
         *                    (may be <code>null</code>)
         * @param now         the current time                        
         * @param dispatcher  the dispatch to capture events to dispatch
         */
        EntryProcessorEntry(K key, RICachedValue cachedValue, long now, RICacheEventEventDispatcher<K, V> dispatcher) {
            this.key = key;
            this.cachedValue = cachedValue;
            this.operation = MutableEntryOperation.NONE;
            this.value = null;
            this.now = now;
            this.dispatcher = dispatcher;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V getValue() {
            if (operation == MutableEntryOperation.NONE) {
                if (cachedValue == null || cachedValue.isExpiredAt(now)) {
                    value = null;
                } else if (value == null) {
                    Object internalValue = cachedValue.getInternalValue(now);
                    value = internalValue == null ? null : (V)RICache.this.valueConverter.fromInternal(internalValue);
                    
                    if (value != null) {
                        dispatcher.addEvent(CacheEntryReadListener.class, new RICacheEntryEvent<K, V>(RICache.this, key, value));
                    }
                }
            }
            
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean exists() {
            return (operation == MutableEntryOperation.NONE && cachedValue != null && !cachedValue.isExpiredAt(now)) || value != null; 
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            operation = cachedValue == null || cachedValue.isExpiredAt(now) ? MutableEntryOperation.NONE : MutableEntryOperation.REMOVE;
            value = null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(V value) {
            if (value == null) {
                throw new NullPointerException();
            }
            operation = cachedValue == null || cachedValue.isExpiredAt(now) ? MutableEntryOperation.CREATE : MutableEntryOperation.UPDATE;
            this.value = value;
        }
    }
}
