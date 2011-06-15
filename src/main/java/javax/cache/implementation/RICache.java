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
import javax.cache.CacheLoader;
import javax.cache.CacheStatisticsMBean;
import javax.cache.Status;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.NotificationScope;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * The reference implementation for JSR107.
 * <p/>
 * This is meant to act as a proof of concept for the API. It is not threadsafe or high performance. It therefore is
 * not suitable for use in production. Please use a production implementation of the API.
 * <p/>
 * This implementation implements all optional parts of JSR107 except for the Transactions chapter. Transactions support
 * simply uses the JTA API. The JSR107 specification details how JTA should be applied to caches.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Greg Luck
 * @author Yannis Cosmadopoulos
 */
public final class RICache<K, V> implements Cache<K, V> {
    private static final int CACHE_LOADER_THREADS = 2;

    private final ConcurrentHashMap<K, V> store = new ConcurrentHashMap<K, V>();
    private final CacheConfiguration configuration;
    private final CacheLoader<K, V> cacheLoader;
    private final ExecutorService executorService = Executors.newFixedThreadPool(CACHE_LOADER_THREADS);
    private volatile Status status;
    private final Set<ScopedListener> cacheEntryListeners = new CopyOnWriteArraySet<ScopedListener>();

    /**
     * Constructs a cache.
     *
     * @param configuration the configuration
     * @param cacheLoader the cache loader
     */
    private RICache(CacheConfiguration configuration, CacheLoader<K, V> cacheLoader) {
        status = Status.UNITIALISED;
        assert configuration != null;
        this.configuration = new RIUnmodifiableCacheConfiguration(configuration);
        this.cacheLoader = cacheLoader;
    }

    /**
     * {@inheritDoc}
     */
    public V get(Object key) throws CacheException {
        checkStatusStarted();
        //noinspection SuspiciousMethodCalls
        return store.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Map<K, V> getAll(Collection<? extends K> keys) {
        checkStatusStarted();
        if (keys.contains(null)) {
            throw new NullPointerException("key");
        }
        // will throw NPE if keys=null
        HashMap<K, V> map = new HashMap<K, V>(keys.size());
        for (K key : keys) {
            map.put(key, store.get(key));
        }
        return map;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        checkStatusStarted();
        //noinspection SuspiciousMethodCalls
        return store.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public Future<V> load(K key, CacheLoader<K, V> specificLoader, Object loaderArgument) {
        checkStatusStarted();
        CacheLoader<K, V> loader = getCacheLoader(specificLoader);
        if (loader == null) {
            return null;
        }
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (store.containsKey(key)) {
            return null;
        }
        FutureTask<V> task = new FutureTask<V>(new RICacheLoaderLoadCallable<K, V>(this, loader, key, loaderArgument));
        executorService.submit(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    public Future<Map<K, V>> loadAll(Collection<? extends K> keys, CacheLoader<K, V> specificLoader, Object loaderArgument) {
        checkStatusStarted();
        if (keys == null) {
            throw new NullPointerException("keys");
        }
        CacheLoader<K, V> loader = getCacheLoader(specificLoader);
        if (loader == null) {
            return null;
        }
        if (keys.contains(null)) {
            throw new NullPointerException("key");
        }
        FutureTask<Map<K, V>> task = new FutureTask<Map<K, V>>(new RICacheLoaderLoadAllCallable<K, V>(this, loader, keys, loaderArgument));
        executorService.submit(task);
        return task;
    }

    private CacheLoader<K, V> getCacheLoader(CacheLoader<K, V> specificLoader) {
        return specificLoader == null ? cacheLoader : specificLoader;
    }

    /**
     * {@inheritDoc}
     */
    public CacheStatisticsMBean getCacheStatistics() {
        checkStatusStarted();
        //TODO: this satisfies API but maybe we want a real impl?
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void put(K key, V value) {
        checkStatusStarted();
        store.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends K, ? extends V> map) {
        checkStatusStarted();
        if (map.containsKey(null)) {
            throw new NullPointerException("key");
        }
        store.putAll(map);
    }

    /**
     * {@inheritDoc}
     */
    public boolean putIfAbsent(K key, V value) {
        checkStatusStarted();
        return store.putIfAbsent(key, value) == null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key) {
        checkStatusStarted();
        //noinspection SuspiciousMethodCalls
        return store.remove(key) != null;
    }

    /**
     * {@inheritDoc}
     */
    public V getAndRemove(Object key) {
        checkStatusStarted();
        //noinspection SuspiciousMethodCalls
        return store.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(K key, V oldValue, V newValue) {
        checkStatusStarted();
        return store.replace(key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(K key, V value) {
        checkStatusStarted();
        return store.replace(key, value) != null;
    }

    /**
     * {@inheritDoc}
     */
    public V getAndReplace(K key, V value) {
        checkStatusStarted();
        return store.replace(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(Collection<? extends K> keys) {
        checkStatusStarted();
        for (K key : keys) {
            store.remove(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() {
        checkStatusStarted();
        store.clear();
    }

    /**
     * {@inheritDoc}
     */
    public CacheConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    public boolean registerCacheEntryListener(CacheEntryListener cacheEntryListener, NotificationScope scope) {
        ScopedListener scopedListener = new ScopedListener(cacheEntryListener, scope);
        return cacheEntryListeners.add(scopedListener);
    }

    /**
     * {@inheritDoc}
     */
    public boolean unregisterCacheEntryListener(CacheEntryListener cacheEntryListener) {
        //Only cacheEntryListener is checked for equality
        ScopedListener scopedListener = new ScopedListener(cacheEntryListener, null);
        return cacheEntryListeners.remove(scopedListener);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Entry<K, V>> iterator() {
        checkStatusStarted();
        return new RIEntryIterator<K, V>(store.entrySet().iterator());
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws CacheException {
        status = Status.STARTED;
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws CacheException {
        status = Status.STOPPING;
        executorService.shutdown();
        //TODO: maybe wait for executor to stop
        store.clear();
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
    public Status getStatus() {
        return status;
    }

    /**
     * Combine a Listener and its NotificationScope.  Equality and hashcode are based purely on the listener.
     * This implies that the same listener cannot be added to the set of registered listeners more than
     * once with different notification scopes.
     *
     * @author Greg Luck
     */
    private static final class ScopedListener {
        private final CacheEntryListener listener;
        private final NotificationScope scope;

        private ScopedListener(CacheEntryListener listener, NotificationScope scope) {
            this.listener = listener;
            this.scope = scope;
        }

        private CacheEntryListener getListener() {
            return listener;
        }

        private NotificationScope getScope() {
            return scope;
        }

        /**
         * Hash code based on listener
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return listener.hashCode();
        }

        /**
         * Equals based on listener (NOT based on scope) - can't have same listener with two different scopes
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ScopedListener other = (ScopedListener) obj;
            if (listener == null) {
                if (other.listener != null) {
                    return false;
                }
            } else if (!listener.equals(other.listener)) {
                return false;
            }
            return true;
        }


        @Override
        public String toString() {
            return listener.toString();
        }
    }

    /**
     * {@inheritDoc}
     * @author Yannis Cosmadopoulos
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

        public K getKey() {
            return key;
        }

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

            RIEntry e2 = (RIEntry) o;

            return  this.getKey().equals(e2.getKey()) &&
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
     * {@inheritDoc}
     * @author Yannis Cosmadopoulos
     */
    private static final class RIEntryIterator<K, V> implements Iterator<Entry<K, V>> {
        private final Iterator<Map.Entry<K, V>> mapIterator;

        private RIEntryIterator(Iterator<Map.Entry<K, V>> mapIterator) {
            this.mapIterator = mapIterator;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return mapIterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public Entry<K, V> next() {
            Map.Entry<K, V> mapEntry = mapIterator.next();
            return new RIEntry<K, V>(mapEntry.getKey(), mapEntry.getValue());
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            mapIterator.remove();
        }
    }

    /**
     * Callable used for cache loader.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @author Yannis Cosmadopoulos
     */
    private static class RICacheLoaderLoadCallable<K, V> implements Callable<V> {
        private final RICache<K, V> cache;
        private final CacheLoader<K, V> cacheLoader;
        private final K key;
        private final Object arg;

        RICacheLoaderLoadCallable(RICache<K, V> cache, CacheLoader<K, V> cacheLoader, K key, Object arg) {
            this.cache = cache;
            this.cacheLoader = cacheLoader;
            this.key = key;
            this.arg = arg;
        }

        public V call() throws Exception {
            V value = cacheLoader.load(key, arg);
            cache.put(key, value);
            return value;
        }
    }

    /**
     * Callable used for cache loader.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @author Yannis Cosmadopoulos
     */
    private static class RICacheLoaderLoadAllCallable<K, V> implements Callable<Map<K, V>> {
        private final RICache<K, V> cache;
        private final CacheLoader<K, V> cacheLoader;
        private final Collection<? extends K> keys;
        private final Object arg;

        RICacheLoaderLoadAllCallable(RICache<K, V> cache, CacheLoader<K, V> cacheLoader, Collection<? extends K> keys, Object arg) {
            this.cache = cache;
            this.cacheLoader = cacheLoader;
            this.keys = keys;
            this.arg = arg;
        }

        public Map<K, V> call() throws Exception {
            ArrayList<K> keysNotInStore = new ArrayList<K>();
            for (K key : keys) {
                if (!cache.store.containsKey(key)) {
                    keysNotInStore.add(key);
                }
            }
            Map<K, V> value = cacheLoader.loadAll(keysNotInStore, arg);
            cache.putAll(value);
            return value;
        }
    }

    /**
     * A Builder for RICache.
     * @param <K>
     * @param <V>
     * @author Yannis Cosmadopoulos
     */
    public static class Builder<K, V> {
        private CacheConfiguration configuration;
        private CacheLoader<K, V> cacheLoader;

        /**
         * Builds the cache
         * @return a constructed cache.
         */
        public RICache<K, V> build() {
            if (configuration == null) {
                configuration = new RICacheConfiguration.Builder().build();
            }
            return new RICache<K, V>(configuration, cacheLoader);
        }

        /**
         *
         * @param configuration the configuration
         * @return the builder
         */
        public Builder<K, V> setCacheConfiguration(CacheConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         *
         * @param cacheLoader the CacheLoader
         * @return the builder
         */
        public Builder<K, V> setCacheLoader(CacheLoader<K, V> cacheLoader) {
            this.cacheLoader = cacheLoader;
            return this;
        }
    }
}
