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

import org.jsr107.ri.processor.EntryProcessorEntry;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.EventType;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;
import javax.cache.integration.CompletionListener;
import javax.cache.management.CacheMXBean;
import javax.cache.management.CacheStatisticsMXBean;
import javax.cache.processor.EntryProcessorException;
import java.io.Closeable;
import java.io.IOException;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static javax.cache.event.EventType.CREATED;
import static javax.cache.event.EventType.EXPIRED;
import static javax.cache.event.EventType.REMOVED;
import static javax.cache.event.EventType.UPDATED;
import static org.jsr107.ri.MBeanServerRegistrationUtility.ObjectNameType.Configuration;
import static org.jsr107.ri.MBeanServerRegistrationUtility.ObjectNameType.Statistics;

/**
 * The reference implementation for JSR107.
 * <p/>
 * This is meant to act as a proof of concept for the API. It is not threadsafe or
 * high performance and does limit
 * the size of caches or provide eviction. It therefore is not suitable for use in production. Please use a
 * production implementation of the API.
 * <p/>
 * This implementation implements all optional parts of JSR107 except for the Transactions chapter. Transactions support
 * simply uses the JTA API. The JSR107 specification details how JTA should be applied to caches.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
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
   * The {@link CacheManager} that created this implementation
   */
  private final RICacheManager cacheManager;

  /**
   * The {@link Configuration} for the {@link Cache}.
   */
  private final MutableConfiguration<K, V> configuration;

  /**
   * The {@link CacheLoader} for the {@link Cache}.
   */
  private CacheLoader<K, V> cacheLoader;

  /**
   * The {@link CacheWriter} for the {@link Cache}.
   */
  private CacheWriter<K, V> cacheWriter;

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
   * The {@link ExpiryPolicy} for the {@link Cache}.
   */
  private final ExpiryPolicy<? super K> expiryPolicy;

  /**
   * The {@link RICacheEntryListenerRegistration}s for the {@link Cache}.
   */
  private final CopyOnWriteArrayList<RICacheEntryListenerRegistration<K, V>> listenerRegistrations;

  /**
   * The open/closed state of the Cache.
   */
  private volatile boolean isClosed;

  private final RICacheMXBean cacheMXBean;
  private final RICacheStatisticsMXBean statistics;

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
   * The dynamic {@link CacheEntryListenerConfiguration}s for the {@link
   * Configuration}.
   */
  private ArrayList<CacheEntryListenerConfiguration<K,
      V>> dynamicListenerConfigurations;


  /**
   * Constructs a cache.
   *
   * @param cacheManager  the CacheManager that's creating the RICache
   * @param cacheName     the name of the Cache
   * @param classLoader   the ClassLoader the RICache will use for loading classes
   * @param configuration the Configuration of the Cache
   */
  RICache(RICacheManager cacheManager,
          String cacheName,
          ClassLoader classLoader,
          Configuration<K, V> configuration) {

    this.cacheManager = cacheManager;
    this.cacheName = cacheName;

    //we make a copy of the configuration here so that the provided one
    //may be changed and or used independently for other caches.  we do this
    //as we don't know if the provided configuration is mutable
    this.configuration = new MutableConfiguration<K, V>(configuration);

    if (this.configuration.getCacheLoaderFactory() != null) {
      cacheLoader = (CacheLoader<K, V>) this.configuration.getCacheLoaderFactory().create();
    }

    if (this.configuration.getCacheWriterFactory() != null) {
      cacheWriter = (CacheWriter<K, V>) this.configuration.getCacheWriterFactory().create();
    }

    keyConverter = configuration.isStoreByValue() ?
        new RISerializingInternalConverter<K>(classLoader) :
        new RIReferenceInternalConverter<K>();

    valueConverter = configuration.isStoreByValue() ?
        new RISerializingInternalConverter<V>(classLoader) :
        new RIReferenceInternalConverter<V>();

    expiryPolicy = configuration.getExpiryPolicyFactory().create();

    entries = new RISimpleInternalMap<Object, RICachedValue>();

    listenerRegistrations = new CopyOnWriteArrayList<RICacheEntryListenerRegistration<K, V>>();
    //establish all of the listeners
    for (CacheEntryListenerConfiguration<K, V> listenerConfiguration : configuration
        .getCacheEntryListenerConfigurations()) {

      createAndAddListener(listenerConfiguration);
    }

    cacheMXBean = new RICacheMXBean<K, V>(this);
    statistics = new RICacheStatisticsMXBean(this);

    //it's important that we set the status BEFORE we let management,
    //statistics and listeners know about the cache.
    isClosed = false;

    if (configuration.isManagementEnabled()) {
      setManagementEnabled(true);
    }

    if (configuration.isStatisticsEnabled()) {
      setStatisticsEnabled(true);
    }
  }

  //todo concurrency
  private void createAndAddListener(CacheEntryListenerConfiguration<K, V> listenerConfiguration) {
    RICacheEntryListenerRegistration<K, V> registration = new RICacheEntryListenerRegistration<K, V>(listenerConfiguration);
    listenerRegistrations.add(registration);
  }

  //todo concurrency
  private void removeListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {

    if (cacheEntryListenerConfiguration == null) {
      throw new NullPointerException("CacheEntryListenerConfiguration can't be null");
    }

    for (RICacheEntryListenerRegistration<K, V> listenerRegistration : listenerRegistrations) {
      if (cacheEntryListenerConfiguration.equals(listenerRegistration.getConfiguration())) {
        listenerRegistrations.remove(listenerRegistration);
        configuration.getCacheEntryListenerConfigurations().remove(cacheEntryListenerConfiguration);
      }
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
   * The default Duration to use when a Duration can't be determined.
   *
   * @return the default Duration
   */
  protected Duration getDefaultDuration() {
    return Duration.ETERNAL;
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
    return cacheManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void close() {
    if (!isClosed) {
      //ensure that any further access to this Cache will raise an IllegalStateException
      isClosed = true;

      //ensure that the cache may no longer be accessed via the CacheManager
      cacheManager.releaseCache(cacheName);

      //disable statistics and management
      setStatisticsEnabled(false);
      setManagementEnabled(false);

      //close the configured CacheLoader
      if (cacheLoader instanceof Closeable) {
        try {
          ((Closeable) cacheLoader).close();
        } catch (IOException e) {
          //TODO: log the exception
        }
      }

      //close the configured CacheWriter
      if (cacheWriter instanceof Closeable) {
        try {
          ((Closeable) cacheWriter).close();
        } catch (IOException e) {
          //TODO: log the exception
        }
      }

      //close the configured ExpiryPolicy
      if (expiryPolicy instanceof Closeable) {
        try {
          ((Closeable) expiryPolicy).close();
        } catch (IOException e) {
          //TODO: log the exception
        }
      }

      //close the configured CacheEntryListeners
      for (RICacheEntryListenerRegistration registration : listenerRegistrations) {
        if (registration.getCacheEntryListener() instanceof Closeable) {
          try {
            ((Closeable) registration).close();
          } catch (IOException e) {
            //TODO: log the exception
          }
        }
      }

      //attempt to shutdown (and wait for the cache to shutdown)
      executorService.shutdown();
      try {
        executorService.awaitTermination(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        throw new CacheException(e);
      }

      //drop all entries from the cache
      entries.clear();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isClosed() {
    return isClosed;
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
    ensureOpen();
    if (key == null) {
      throw new NullPointerException();
    }

    RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();

    V value = getValue(key, dispatcher);

    dispatcher.dispatch(listenerRegistrations);

    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<K, V> getAll(Set<? extends K> keys) {
    ensureOpen();
    if (keys.contains(null)) {
      throw new NullPointerException("key");
    }
    // will throw NPE if keys=null
    HashMap<K, V> map = new HashMap<K, V>(keys.size());

    RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();

    for (K key : keys) {
      V value = getValue(key, dispatcher);
      if (value != null) {
        map.put(key, value);
      }
    }

    dispatcher.dispatch(listenerRegistrations);

    return map;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKey(K key) {
    ensureOpen();
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
  public void loadAll(final Set<? extends K> keys, final boolean replaceExistingValues, final CompletionListener completionListener) {
    ensureOpen();
    if (keys == null) {
      throw new NullPointerException("keys");
    }

    if (cacheLoader == null) {
      if (completionListener != null) {
        completionListener.onCompletion();
      }
    } else {
      for (K key : keys) {
        if (key == null) {
          throw new NullPointerException("keys contains a null");
        }
      }

      executorService.submit(new Runnable() {
        @Override
        public void run() {
          try {
            ArrayList<K> keysToLoad = new ArrayList<K>();
            for (K key : keys) {
              if (replaceExistingValues || !containsKey(key)) {
                keysToLoad.add(key);
              }
            }

            Map<? extends K, ? extends V> loaded;
            try {
              loaded = cacheLoader.loadAll(keysToLoad);
            } catch (Exception e) {
              if (!(e instanceof CacheLoaderException)) {
                throw new CacheLoaderException("Exception in CacheLoader", e);
              } else {
                throw e;
              }
            }

            for (K key : keysToLoad) {
              if (loaded.get(key) == null) {
                loaded.remove(key);
              }
            }

            putAll(loaded, replaceExistingValues, false);

            if (completionListener != null) {
              completionListener.onCompletion();
            }
          } catch (Exception e) {
            if (completionListener != null) {
              completionListener.onException(e);
            }
          }
        }
      });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(K key, V value) {
    long start = statisticsEnabled() ? System.nanoTime() : 0;
    ensureOpen();
    if (value == null) {
      throw new NullPointerException("null value specified for key " + key);
    }

    lockManager.lock(key);
    try {
      RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();

      long now = System.currentTimeMillis();

      Object internalKey = keyConverter.toInternal(key);
      Object internalValue = valueConverter.toInternal(value);

      RICachedValue cachedValue = entries.get(internalKey);

      boolean isExpired = cachedValue != null && cachedValue.isExpiredAt(now);
      if (cachedValue == null || isExpired) {

        RIEntry<K, V> entry = new RIEntry<K, V>(key, value);
        writeCacheEntry(entry);

        if (isExpired) {
          V expiredValue = valueConverter.fromInternal(cachedValue.get());
          dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue, EXPIRED));
        }

        Duration duration;
        try {
          duration = expiryPolicy.getExpiryForCreatedEntry(key);
        } catch (Throwable t) {
          duration = getDefaultDuration();
        }
        long expiryTime = duration.getAdjustedTime(now);

        cachedValue = new RICachedValue(internalValue, now, expiryTime);

        entries.put(internalKey, cachedValue);

        dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, EventType.CREATED));

      } else {

        V oldValue = valueConverter.fromInternal(cachedValue.get());
        RIEntry<K, V> entry = new RIEntry<K, V>(key, value, oldValue);

        writeCacheEntry(entry);

        try {
          Duration duration = expiryPolicy.getExpiryForModifiedEntry(key);
          if (duration != null) {
            long expiryTime = duration.getAdjustedTime(now);
            cachedValue.setExpiryTime(expiryTime);
          }
        } catch (Throwable t) {
          //leave the expiry time untouched when we can't determine a duration
        }

        cachedValue.setInternalValue(internalValue, now);

        dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, oldValue, EventType.UPDATED));
      }

      dispatcher.dispatch(listenerRegistrations);

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
    ensureOpen();
    if (value == null) {
      throw new NullPointerException("null value specified for key " + key);
    }

    long start = statisticsEnabled() ? System.nanoTime() : 0;
    long now = System.currentTimeMillis();

    V result;
    lockManager.lock(key);
    try {
      RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();

      Object internalKey = keyConverter.toInternal(key);
      Object internalValue = valueConverter.toInternal(value);

      RICachedValue cachedValue = entries.get(internalKey);

      boolean isExpired = cachedValue != null && cachedValue.isExpiredAt(now);
      if (cachedValue == null || isExpired) {

        RIEntry<K, V> entry = new RIEntry<K, V>(key, value);
        writeCacheEntry(entry);

        if (isExpired) {
          V expiredValue = valueConverter.fromInternal(cachedValue.get());
          dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue, EXPIRED));
        }

        Duration duration;
        try {
          duration = expiryPolicy.getExpiryForCreatedEntry(key);
        } catch (Throwable t) {
          duration = getDefaultDuration();
        }
        long expiryTime = duration.getAdjustedTime(now);

        cachedValue = new RICachedValue(internalValue, now, expiryTime);
        entries.put(internalKey, cachedValue);
        result = null;

        dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, CREATED));

        //TODO: count the "miss" in the statistics

      } else {
        V oldValue = valueConverter.fromInternal(cachedValue.getInternalValue(now));
        RIEntry<K, V> entry = new RIEntry<K, V>(key, value, oldValue);
        writeCacheEntry(entry);

        try {
          Duration duration = expiryPolicy.getExpiryForModifiedEntry(key);
          if (duration != null) {
            long expiryTime = duration.getAdjustedTime(now);
            cachedValue.setExpiryTime(expiryTime);
          }
        } catch (Throwable t) {
          //leave the expiry time untouched when we can't determine a duration
        }
        cachedValue.setInternalValue(internalValue, now);

        result = oldValue;

        dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, oldValue, UPDATED));
      }

      dispatcher.dispatch(listenerRegistrations);

    } finally {
      lockManager.unLock(key);
    }
    if (statisticsEnabled()) {
      statistics.increaseCachePuts(1);
      statistics.addPutTimeNano(System.nanoTime() - start);
    }
    if (result != null) {
      statistics.increaseCacheHits(1);
      statistics.addGetTimeNano(System.nanoTime() - start);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    putAll(map, true);
  }

  /**
   */
  public void putAll(Map<? extends K, ? extends V> map, boolean replaceExistingValues) {
    putAll(map, replaceExistingValues, true);
  }

  /**
   * A implementation of PutAll that allows optional replacement of existing
   * values and optionally writing values when Write Through is configured.
   *
   * @param map                   the Map of entries to put
   * @param replaceExistingValues should existing values be replaced by those in the map?
   * @param useWriteThrough       should write-through be used if it is configured
   */
  public void putAll(Map<? extends K, ? extends V> map, final boolean replaceExistingValues, boolean useWriteThrough) {
    ensureOpen();
    long start = statisticsEnabled() ? System.nanoTime() : 0;

    long now = System.currentTimeMillis();

    if (map.containsKey(null)) {
      throw new NullPointerException("key");
    }

    CacheWriterException exception = null;

    RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();

    try {
      boolean isWriteThrough = configuration.isWriteThrough() && cacheWriter != null && useWriteThrough;

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
          cacheWriter.writeAll(entriesToWrite);
        } catch (Exception e) {
          if (!(e instanceof CacheWriterException)) {
            exception = new CacheWriterException("Exception during write", e);
          }
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
            dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue, EXPIRED));
          }

          Duration duration;
          try {
            duration = expiryPolicy.getExpiryForCreatedEntry(key);
          } catch (Throwable t) {
            duration = getDefaultDuration();
          }
          long expiryTime = duration.getAdjustedTime(now);

          cachedValue = new RICachedValue(internalValue, now, expiryTime);

          entries.put(internalKey, cachedValue);

          dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, CREATED));
        } else if (replaceExistingValues) {
          V oldValue = valueConverter.fromInternal(cachedValue.get());

          try {
            Duration duration = expiryPolicy.getExpiryForModifiedEntry(key);
            if (duration != null) {
              long expiryTime = duration.getAdjustedTime(now);
              cachedValue.setExpiryTime(expiryTime);
            }
          } catch (Throwable t) {
            //leave the expiry time untouched when we can't determine a duration
          }

          cachedValue.setInternalValue(internalValue, now);

          dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, oldValue, UPDATED));
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
    dispatcher.dispatch(listenerRegistrations);

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
    ensureOpen();
    if (value == null) {
      throw new NullPointerException("null value specified for key " + key);
    }

    long start = statisticsEnabled() ? System.nanoTime() : 0;

    long now = System.currentTimeMillis();

    boolean result;
    lockManager.lock(key);
    try {
      RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();

      Object internalKey = keyConverter.toInternal(key);
      Object internalValue = valueConverter.toInternal(value);

      RICachedValue cachedValue = entries.get(internalKey);

      boolean isExpired = cachedValue != null && cachedValue.isExpiredAt(now);
      if (cachedValue == null || cachedValue.isExpiredAt(now)) {

        RIEntry<K, V> entry = new RIEntry<K, V>(key, value);
        writeCacheEntry(entry);

        if (isExpired) {
          V expiredValue = valueConverter.fromInternal(cachedValue.get());
          dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue, EXPIRED));
        }

        Duration duration;
        try {
          duration = expiryPolicy.getExpiryForCreatedEntry(key);
        } catch (Throwable t) {
          duration = getDefaultDuration();
        }
        long expiryTime = duration.getAdjustedTime(now);

        cachedValue = new RICachedValue(internalValue, now, expiryTime);
        entries.put(internalKey, cachedValue);
        result = true;

        dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, CREATED));
      } else {
        result = false;
      }

      dispatcher.dispatch(listenerRegistrations);

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
    ensureOpen();
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

        RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();
        dispatcher.addEvent(CacheEntryRemovedListener.class, new RICacheEntryEvent<K, V>(this, key, value, REMOVED));
        dispatcher.dispatch(listenerRegistrations);

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
    ensureOpen();
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

          RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();
          dispatcher.addEvent(CacheEntryRemovedListener.class, new RICacheEntryEvent<K, V>(this, key, oldValue, REMOVED));
          dispatcher.dispatch(listenerRegistrations);

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
    ensureOpen();

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

        RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();
        dispatcher.addEvent(CacheEntryRemovedListener.class, new RICacheEntryEvent<K, V>(this, key, result, REMOVED));
        dispatcher.dispatch(listenerRegistrations);
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
    ensureOpen();
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

          RIEntry<K, V> entry = new RIEntry<K, V>(key, newValue, oldValue);
          writeCacheEntry(entry);

          try {
            Duration duration = expiryPolicy.getExpiryForModifiedEntry(key);
            if (duration != null) {
              long expiryTime = duration.getAdjustedTime(now);
              cachedValue.setExpiryTime(expiryTime);
            }
          } catch (Throwable t) {
            //leave the expiry time untouched when we can't determine a duration
          }

          Object newInternalValue = valueConverter.toInternal(newValue);
          cachedValue.setInternalValue(newInternalValue, now);

          RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();
          dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, newValue, oldValue, UPDATED));
          dispatcher.dispatch(listenerRegistrations);

          result = true;
        } else {
          try {
            RIEntry<K, V> entry = new RIEntry<K, V>(key, oldValue);
            Duration duration = expiryPolicy.getExpiryForAccessedEntry(key);
            if (duration != null) {
              long expiryTime = duration.getAdjustedTime(now);
              cachedValue.setExpiryTime(expiryTime);
            }
          } catch (Throwable t) {
            //leave the expiry time untouched when we can't determine a duration
          }

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
    ensureOpen();
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
        V oldValue = valueConverter.fromInternal(cachedValue.get());

        RIEntry<K, V> entry = new RIEntry<K, V>(key, value, oldValue);
        writeCacheEntry(entry);

        try {
          Duration duration = expiryPolicy.getExpiryForModifiedEntry(key);
          if (duration != null) {
            long expiryTime = duration.getAdjustedTime(now);
            cachedValue.setExpiryTime(expiryTime);
          }
        } catch (Throwable t) {
          //leave the expiry time untouched when we can't determine a duration
        }

        Object internalValue = valueConverter.toInternal(value);
        cachedValue.setInternalValue(internalValue, now);

        RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();
        dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, oldValue, UPDATED));
        dispatcher.dispatch(listenerRegistrations);

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
    ensureOpen();
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
        V oldValue = valueConverter.fromInternal(cachedValue.getInternalValue(now));
        RIEntry<K, V> entry = new RIEntry<K, V>(key, value, oldValue);
        writeCacheEntry(entry);

        try {
          Duration duration = expiryPolicy.getExpiryForModifiedEntry(key);
          if (duration != null) {
            long expiryTime = duration.getAdjustedTime(now);
            cachedValue.setExpiryTime(expiryTime);
          }
        } catch (Throwable t) {
          //leave the expiry time untouched when we can't determine a duration
        }

        Object internalValue = valueConverter.toInternal(value);
        cachedValue.setInternalValue(internalValue, now);

        RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();
        dispatcher.addEvent(CacheEntryUpdatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, oldValue, UPDATED));
        dispatcher.dispatch(listenerRegistrations);

        result = oldValue;
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
    ensureOpen();

    long now = System.currentTimeMillis();

    CacheException exception = null;
    HashSet<K> lockedKeys = new HashSet<K>();

    RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();

    try {
      boolean isWriteThrough = configuration.isWriteThrough() && cacheWriter != null;

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
          cacheWriter.deleteAll(keysToDelete);
        } catch (Exception e) {
          if (!(e instanceof CacheWriterException)) {
            exception = new CacheWriterException("Exception during write", e);
          }
        }
      }

      //remove the deleted keys that were successfully deleted
      for (K key : lockedKeys) {
        if (!keysToDelete.contains(key)) {
          Object internalKey = keyConverter.toInternal(key);
          RICachedValue cachedValue = entries.remove(internalKey);

          V value = valueConverter.fromInternal(cachedValue.get());

          if (cachedValue.isExpiredAt(now)) {
            dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, value, EXPIRED));
          } else {
            dispatcher.addEvent(CacheEntryRemovedListener.class, new RICacheEntryEvent<K, V>(this, key, value, REMOVED));
          }
        }
      }

    } finally {
      //unlock all of the keys
      for (K key : lockedKeys) {
        lockManager.unLock(key);
      }
    }

    dispatcher.dispatch(listenerRegistrations);

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
    ensureOpen();

    //TODO: this is incorrect as the internal map may contain expired entries
    int size = (statisticsEnabled()) ? entries.size() : 0;

    long now = System.currentTimeMillis();

    CacheException exception = null;
    HashSet<K> lockedKeys = new HashSet<K>();

    RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();

    try {
      boolean isWriteThrough = configuration.isWriteThrough() && cacheWriter != null;

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
          cacheWriter.deleteAll(keysToDelete);
        } catch (Exception e) {
          if (!(e instanceof CacheWriterException)) {
            exception = new CacheWriterException("Exception during write", e);
          }
        }
      }

      //remove the deleted keys that were successfully deleted from the set
      for (K key : lockedKeys) {
        if (!keysToDelete.contains(key)) {
          Object internalKey = keyConverter.toInternal(key);
          RICachedValue cachedValue = entries.remove(internalKey);

          V value = valueConverter.fromInternal(cachedValue.get());


          if (cachedValue.isExpiredAt(now)) {
            dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, value, EXPIRED));
          } else {
            dispatcher.addEvent(CacheEntryRemovedListener.class, new RICacheEntryEvent<K, V>(this, key, value, REMOVED));
          }
        }
      }

    } finally {
      //unlock all of the keys
      for (K key : lockedKeys) {
        lockManager.unLock(key);
      }
    }

    dispatcher.dispatch(listenerRegistrations);

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
    ensureOpen();

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
  public <T> T invoke(K key, javax.cache.processor.EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
    ensureOpen();
    if (key == null) {
      throw new NullPointerException();
    }
    if (entryProcessor == null) {
      throw new NullPointerException();
    }

    long start = statisticsEnabled() ? System.nanoTime() : 0;


    T result = null;
    lockManager.lock(key);
    try {
      long now = System.currentTimeMillis();

      RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();

      Object internalKey = keyConverter.toInternal(key);
      RICachedValue cachedValue = entries.get(internalKey);
      if (statisticsEnabled()) {
        if (cachedValue == null) {
          statistics.increaseCacheMisses(1);
        } else {
          statistics.increaseCacheHits(1);
        }
      }
      if (statisticsEnabled()) {
        statistics.addGetTimeNano(System.nanoTime() - start);
      }
      //restart start as fetch finished
      start = statisticsEnabled() ? System.nanoTime() : 0;

      EntryProcessorEntry<K, V> entry = new EntryProcessorEntry<>(valueConverter, key,
        cachedValue, now, dispatcher, configuration.isReadThrough() ? cacheLoader : null);
      try {
        result = entryProcessor.process(entry, arguments);
      } catch (Exception e) {
        if (!(e instanceof EntryProcessorException)) {
          throw new EntryProcessorException(e);
        } else {
          throw e;
        }
      }

      Duration duration;
      long expiryTime;
      switch (entry.getOperation()) {
        case NONE:
          break;

        case ACCESS:
          // update Access ExpiryPolicy for entry.
          try {
            duration = expiryPolicy.getExpiryForAccessedEntry(key);
            if (duration != null) {
              long expiryTime1 = duration.getAdjustedTime(now);
              cachedValue.setExpiryTime(expiryTime1);
            }
          } catch (Throwable t) {
            //leave the expiry time untouched when we can't determine a duration
          }
          break;

        case CREATE:
          RIEntry<K, V> e = new RIEntry<K, V>(key, entry.getValue());
          writeCacheEntry(e);

          try {
            duration = expiryPolicy.getExpiryForCreatedEntry(key);
          } catch (Throwable t) {
            duration = getDefaultDuration();
          }
          expiryTime = duration.getAdjustedTime(now);

          cachedValue = new RICachedValue(valueConverter.toInternal(entry.getValue()),
              now, expiryTime);

          if (cachedValue.isExpiredAt(now)) {
            V previousValue = valueConverter.fromInternal(cachedValue.get());
            dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, previousValue, EXPIRED));
          }

          entries.put(internalKey, cachedValue);

          dispatcher.addEvent(CacheEntryCreatedListener.class,
              new RICacheEntryEvent<K, V>(this, key, entry.getValue(), CREATED));

          if (statisticsEnabled()) {
            statistics.increaseCachePuts(1);
            statistics.addPutTimeNano(System.nanoTime() - start);
          }

          break;

        case UPDATE:
          V oldValue = valueConverter.fromInternal(cachedValue.get());

          e = new RIEntry<K, V>(key, entry.getValue(), oldValue);
          writeCacheEntry(e);

          try {
            duration = expiryPolicy.getExpiryForModifiedEntry(key);
            if (duration != null) {
              expiryTime = duration.getAdjustedTime(now);
              cachedValue.setExpiryTime(expiryTime);
            }
          } catch (Throwable t) {
            //leave the expiry time untouched when we can't determine a duration
          }

          cachedValue.setInternalValue(valueConverter.toInternal(entry.getValue()), now);

          dispatcher.addEvent(CacheEntryUpdatedListener.class,
              new RICacheEntryEvent<K, V>(this, key, entry.getValue(), oldValue,
                  UPDATED));

          if (statisticsEnabled()) {
            statistics.increaseCachePuts(1);
            statistics.addPutTimeNano(System.nanoTime() - start);
          }

          break;

        case REMOVE:
          deleteCacheEntry(key);

          oldValue = cachedValue == null ? null : valueConverter.fromInternal(cachedValue.get());
          entries.remove(internalKey);

          dispatcher.addEvent(CacheEntryRemovedListener.class, new RICacheEntryEvent<K, V>(this, key, oldValue, REMOVED));

          if (statisticsEnabled()) {
            statistics.increaseCacheRemovals(1);
            statistics.addRemoveTimeNano(System.nanoTime() - start);
          }

          break;

        default:
          break;
      }

      dispatcher.dispatch(listenerRegistrations);

    } finally {
      lockManager.unLock(key);
    }
    return result;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Map<K, T> invokeAll(Set<? extends K> keys,
                                 javax.cache.processor.EntryProcessor<K, V, T> entryProcessor,
                                 Object... arguments) {

    ensureOpen();
    if (keys == null) {
      throw new NullPointerException();
    }
    if (entryProcessor == null) {
      throw new NullPointerException();
    }

    HashMap<K, T> map = new HashMap<K, T>();
    for (K key : keys) {
      T t = invoke(key, entryProcessor, arguments);
      if (t != null) {
        map.put(key, t);
      }
    }

    return map;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<Entry<K, V>> iterator() {
    ensureOpen();

    long now = System.currentTimeMillis();

    return new RIEntryIterator(entries.iterator(), now);
  }

  /**
   * @return the managemtn bean
   */

  public CacheMXBean getCacheMXBean() {
    return cacheMXBean;
  }


  /**
   * @return the managemtn bean
   */
  public CacheStatisticsMXBean getCacheStatisticsMXBean() {
    return statistics;
  }


  /**
   * Sets statistics
   */
  public void setStatisticsEnabled(boolean enabled) {
    if (enabled) {
      MBeanServerRegistrationUtility.registerCacheObject(this, Statistics);
    } else {
      MBeanServerRegistrationUtility.unregisterCacheObject(this, Statistics);
    }
    configuration.setStatisticsEnabled(enabled);
  }


  /**
   * Sets management enablement
   *
   * @param enabled true if management should be enabled
   */
  public void setManagementEnabled(boolean enabled) {
    if (enabled) {
      MBeanServerRegistrationUtility.registerCacheObject(this, Configuration);
    } else {
      MBeanServerRegistrationUtility.unregisterCacheObject(this, Configuration);
    }
    configuration.setManagementEnabled(enabled);
  }

  private void ensureOpen() {
    if (isClosed()) {
      throw new IllegalStateException("Cache operations can not be performed. The cache closed");
    }
  }

  @Override
  public <T> T unwrap(java.lang.Class<T> cls) {
    if (cls.isAssignableFrom(((Object) this).getClass())) {
      return cls.cast(this);
    }

    throw new IllegalArgumentException("Unwrapping to " + cls + " is not a supported by this implementation");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
    configuration.addCacheEntryListenerConfiguration(cacheEntryListenerConfiguration);
    createAndAddListener(cacheEntryListenerConfiguration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
    removeListener(cacheEntryListenerConfiguration);
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
      try {
        cacheWriter.write(entry);
      } catch (Exception e) {
        if (!(e instanceof CacheWriterException)) {
          throw new CacheWriterException("Exception in CacheWriter", e);
        } else {
          throw e;
        }
      }
    }
  }

  /**
   * Deletes the Cache Entry using the configued CacheWriter.  Does nothing
   * if write-through is not configued.
   *
   * @param key
   */
  private void deleteCacheEntry(K key) {
    if (configuration.isWriteThrough()) {
      try {
        cacheWriter.delete(key);
      } catch (Exception e) {
        if (!(e instanceof CacheWriterException)) {
          throw new CacheWriterException("Exception in CacheWriter", e);
        } else {
          throw e;
        }
      }
    }
  }

  /**
   * Gets the value for the specified key from the underlying cache, including
   * attempting to load it if a CacheLoader is configured (with read-through).
   * <p/>
   * Any events that need to be raised are added to the specified dispatcher.
   *
   * @param key        the key of the entry to get from the cache
   * @param dispatcher the dispatcher for events
   * @return the value loaded
   */
  private V getValue(K key, RICacheEventDispatcher<K, V> dispatcher) {
    long now = System.currentTimeMillis();
    long start = statisticsEnabled() ? System.nanoTime() : 0;


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
          dispatcher.addEvent(CacheEntryExpiredListener.class, new RICacheEntryEvent<K, V>(this, key, expiredValue, EXPIRED));
        }

        if (statisticsEnabled()) {
          statistics.increaseCacheMisses(1);
        }

        Entry<K, ? extends V> entry = null;
        if (configuration.isReadThrough() && cacheLoader != null) {
          try {
            entry = cacheLoader.load(key);
          } catch (Exception e) {
            if (!(e instanceof CacheLoaderException)) {
              throw new CacheLoaderException("Exception in CacheLoader", e);
            } else {
              throw e;
            }
          }
        }

        if (entry == null) {
          return null;
        }

        value = entry.getValue();

        Duration duration;
        try {
          duration = expiryPolicy.getExpiryForCreatedEntry(key);
        } catch (Throwable t) {
          duration = getDefaultDuration();
        }
        long expiryTime = duration.getAdjustedTime(now);

        Object internalValue = valueConverter.toInternal(value);
        cachedValue = new RICachedValue(internalValue, now, expiryTime);

        if (cachedValue.isExpiredAt(now)) {
          return null;
        } else {
          entries.put(internalKey, cachedValue);

          dispatcher.addEvent(CacheEntryCreatedListener.class, new RICacheEntryEvent<K, V>(this, key, value, CREATED));
        }
      } else {
        value = valueConverter.fromInternal(cachedValue.getInternalValue(now));
        RIEntry<K, V> entry = new RIEntry<K, V>(key, value);

        try {
          Duration duration = expiryPolicy.getExpiryForAccessedEntry(key);
          if (duration != null) {
            long expiryTime = duration.getAdjustedTime(now);
            cachedValue.setExpiryTime(expiryTime);
          }
        } catch (Throwable t) {
          //leave the expiry time untouched when we can't determine a duration
        }

        if (statisticsEnabled()) {
          statistics.increaseCacheHits(1);
        }
      }

    } finally {
      lockManager.unLock(key);
      if (statisticsEnabled()) {
        statistics.addGetTimeNano(System.nanoTime() - start);
      }
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
    private final V oldValue;

    public RIEntry(K key, V value) {
      if (key == null) {
        throw new NullPointerException("key");
      }
      if (value == null) {
        throw new NullPointerException("value");
      }

      this.key = key;
      this.value = value;
      this.oldValue = null;
    }

    public RIEntry(K key, V value, V oldValue) {
      if (key == null) {
        throw new NullPointerException("key");
      }
      if (value == null) {
        throw new NullPointerException("value");
      }
      if (oldValue == null) {
        throw new NullPointerException("oldValue");
      }

      this.key = key;
      this.value = value;
      this.oldValue = oldValue;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    public V getOldValue() {
      return oldValue;
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
      if (clazz != null && clazz.isInstance(this)) {
        return (T) this;
      } else {
        throw new IllegalArgumentException("Class " + clazz + " is unknown to this implementation");
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || ((Object) this).getClass() != o.getClass()) return false;

      RIEntry<?, ?> e2 = (RIEntry<?, ?>) o;

      return this.getKey().equals(e2.getKey()) &&
          this.getValue().equals(e2.getValue()) &&
          (this.oldValue == null && e2.oldValue == null ||
              this.getOldValue().equals(e2.getOldValue()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return getKey().hashCode();
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

        K key = (K) RICache.this.keyConverter.fromInternal(entry.getKey());
        lockManager.lock(key);
        try {
          if (!cachedValue.isExpiredAt(now)) {
            V value = (V) RICache.this.valueConverter.fromInternal(cachedValue.getInternalValue(now));
            nextEntry = new RIEntry<K, V>(key, value);

            try {
              Duration duration = expiryPolicy.getExpiryForAccessedEntry(key);
              if (duration != null) {
                long expiryTime = duration.getAdjustedTime(now);
                cachedValue.setExpiryTime(expiryTime);
              }
            } catch (Throwable t) {
              //leave the expiry time untouched when we can't determine a duration
            }
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
          RICacheEventDispatcher<K, V> dispatcher = new RICacheEventDispatcher<K, V>();
          dispatcher.addEvent(CacheEntryRemovedListener.class,
              new RICacheEntryEvent<K, V>(RICache.this, lastEntry.getKey(), lastEntry.getValue(), REMOVED));
          dispatcher.dispatch(listenerRegistrations);

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
      Map<K, ? extends V> value;
      try {
        value = cacheLoader.loadAll(keysNotInStore);
      } catch (Exception e) {
        if (!(e instanceof CacheLoaderException)) {
          throw new CacheLoaderException("Exception in CacheLoader", e);
        } else {
          throw e;
        }
      }

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
     *
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
     *
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


}
