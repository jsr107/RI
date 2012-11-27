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
import javax.cache.CacheLoader;
import javax.cache.CacheStatistics;
import javax.cache.Status;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryFilter;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryReadListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.mbeans.CacheMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
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
 * @author Greg Luck
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public final class RICache<K, V> extends AbstractCache<K, V> {

    private final RISimpleCache<K, V> store;
    private final Set<CacheEntryListener<? super K, ? super V>> cacheEntryListeners =
            new CopyOnWriteArraySet<CacheEntryListener<? super K, ? super V>>();
    private volatile Status status;
    private final RICacheStatistics statistics;
    private final CacheMXBean mBean;
    private final LockManager<K> lockManager = new LockManager<K>();

    /**
     * Constructs a cache.
     *
     * @param cacheName        the cache name
     * @param cacheManagerName the cache manager name
     * @param classLoader      the class loader
     * @param configuration    the configuration
     */
    RICache(String cacheName,
            String cacheManagerName,
            ClassLoader classLoader,
            CacheConfiguration<K, V> configuration) {

        super(cacheName, cacheManagerName, classLoader, configuration);

        status = Status.UNINITIALISED;
        store = configuration.isStoreByValue() ?
                new RIByValueSimpleCache<K, V>(new RISerializer<K>(classLoader),
                        new RISerializer<V>(classLoader)) :
                new RIByReferenceSimpleCache<K, V>();

        statistics = new RICacheStatistics(this);

        mBean = new DelegatingCacheMXBean<K, V>(this);

        for (CacheEntryListener<? super K, ? super V> listener : configuration.getCacheEntryListeners()) {
            registerCacheEntryListener(listener, false, null, true);
        }
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
        V value = getInternal(key);
        for (CacheEntryListener<? super K, ? super V> cacheEntryListener : cacheEntryListeners) {
            if (cacheEntryListener instanceof CacheEntryReadListener) {
                ArrayList events = new ArrayList<CacheEntryEvent<K, V>>();
                events.add(new RICacheEntryEvent<K, V>(this, key, value, null));
                ((CacheEntryReadListener<K, V>) cacheEntryListener).onRead(events);
            }
        }
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
        ArrayList events = new ArrayList<CacheEntryEvent<K, V>>();
        HashMap<K, V> map = new HashMap<K, V>(keys.size());
        for (K key : keys) {
            V value = getInternal(key);
            if (value != null) {
                map.put(key, value);
                //listener only triggered when not null.
                events.add(new RICacheEntryEvent<K, V>(this, key, value, null));
            }
        }
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
        lockManager.lock(key);
        try {
            return store.containsKey(key);
        } finally {
            lockManager.unLock(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<V> load(K key) {
        checkStatusStarted();
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (getCacheLoader() == null) {
            return null;
        }
        if (containsKey(key)) {
            return null;
        }
        FutureTask<V> task = new FutureTask<V>(new RICacheLoaderLoadCallable<K, V>(this, getCacheLoader(), key));
        submit(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Future<Map<K, ? extends V>> loadAll(Set<? extends K> keys) {
        checkStatusStarted();
        if (keys == null) {
            throw new NullPointerException("keys");
        }
        if (getCacheLoader() == null) {
            return null;
        }
        if (keys.contains(null)) {
            throw new NullPointerException("key");
        }
        Callable<Map<K, ? extends V>> callable = new RICacheLoaderLoadAllCallable<K, V>(this, getCacheLoader(), keys);
        FutureTask<Map<K, ? extends V>> task = new FutureTask<Map<K, ? extends V>>(callable);
        submit(task);
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheStatistics getStatistics() {
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
        //in-process makes not performance difference
        getAndPut(key, value);
    }

    @Override
    public V getAndPut(K key, V value) {
        checkStatusStarted();
        long start = statisticsEnabled() ? System.nanoTime() : 0;
        V oldValue;
        lockManager.lock(key);
        try {
            oldValue = store.getAndPut(key, value);
        } finally {
            lockManager.unLock(key);
        }

        if (oldValue == null) {
            for (CacheEntryListener<? super K, ? super V> cacheEntryListener : cacheEntryListeners) {
                if (cacheEntryListener instanceof CacheEntryCreatedListener) {
                    ArrayList events = new ArrayList<CacheEntryEvent<K, V>>();
                    events.add(new RICacheEntryEvent<K, V>(this, key, value, oldValue));
                    ((CacheEntryCreatedListener<K, V>) cacheEntryListener).onCreated(events);
                }
            }
        } else {
            for (CacheEntryListener<? super K, ? super V> cacheEntryListener : cacheEntryListeners) {
                if (cacheEntryListener instanceof CacheEntryUpdatedListener) {
                    ArrayList events = new ArrayList<CacheEntryEvent<K, V>>();
                    events.add(new RICacheEntryEvent<K, V>(this, key, value, oldValue));
                    ((CacheEntryUpdatedListener<K, V>) cacheEntryListener).onUpdated(events);
                }
            }
        }

        if (statisticsEnabled()) {
            statistics.increaseCachePuts(1);
            statistics.addPutTimeNano(System.nanoTime() - start);
        }
        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        checkStatusStarted();
        long start = statisticsEnabled() ? System.nanoTime() : 0;
        if (map.containsKey(null)) {
            throw new NullPointerException("key");
        }
        //store.putAll(map);
        ArrayList newEvents = new ArrayList<CacheEntryEvent<K, V>>();
        ArrayList replaceEvents = new ArrayList<CacheEntryEvent<K, V>>();
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            K key = entry.getKey();
            lockManager.lock(key);
            try {
                V oldValue = store.put(key, entry.getValue());
                if (oldValue == null) {
                    newEvents.add(new RICacheEntryEvent<K, V>(this, key, entry.getValue(), oldValue));
                } else {
                    replaceEvents.add(new RICacheEntryEvent<K, V>(this, key, entry.getValue(), oldValue));
                }
            } finally {
                lockManager.unLock(key);
            }
        }

        //call listeners
        for (CacheEntryListener<? super K, ? super V> cacheEntryListener : cacheEntryListeners) {
            if (cacheEntryListener instanceof CacheEntryCreatedListener) {
                ((CacheEntryCreatedListener<K, V>) cacheEntryListener).onCreated(newEvents);
            }
            if (cacheEntryListener instanceof CacheEntryUpdatedListener) {
                ((CacheEntryUpdatedListener<K, V>) cacheEntryListener).onUpdated(replaceEvents);
            }
        }

        if (statisticsEnabled()) {
            statistics.increaseCachePuts(map.size());
            statistics.addPutTimeNano(System.nanoTime() - start);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putIfAbsent(K key, V value) {
        checkStatusStarted();
        long start = statisticsEnabled() ? System.nanoTime() : 0;
        boolean result;
        lockManager.lock(key);
        try {
            result = store.putIfAbsent(key, value);
        } finally {
            lockManager.unLock(key);
        }
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
        boolean result;
        lockManager.lock(key);
        try {
            result = store.remove(key);
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
        long start = statisticsEnabled() ? System.nanoTime() : 0;
        boolean result;
        lockManager.lock(key);
        try {
            result = store.remove(key, oldValue);
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
        V result;
        lockManager.lock(key);
        try {
            result = store.getAndRemove(key);
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
        boolean result;
        lockManager.lock(key);
        try {
            result = store.replace(key, oldValue, newValue);
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
        boolean result;
        lockManager.lock(key);
        try {
            result = store.replace(key, value);
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
        V result;
        lockManager.lock(key);
        try {
            result = store.getAndReplace(key, value);
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
        for (K key : keys) {
            lockManager.lock(key);
            try {
                store.remove(key);
            } finally {
                lockManager.unLock(key);
            }
        }
        if (statisticsEnabled()) {
            statistics.increaseCacheRemovals(keys.size());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() {
        checkStatusStarted();
        int size = (statisticsEnabled()) ? store.size() : 0;
        //store.removeAll();
        Iterator<Map.Entry<K, V>> iterator = store.iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, V> entry = iterator.next();
            K key = entry.getKey();
            lockManager.lock(key);
            try {
                iterator.remove();
            } finally {
                lockManager.unLock(key);
            }
        }
        //possible race here but it is only stats
        if (statisticsEnabled()) {
            statistics.increaseCacheRemovals(size);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean registerCacheEntryListener(CacheEntryListener<? super K, ? super V> cacheEntryListener,
                                              boolean requireOldValue,
                                              CacheEntryFilter<K, V> cacheEntryFilter,
                                              boolean synchronous) {
        if (cacheEntryListener == null) {
            throw new CacheEntryListenerException("A listener may not be null");
        }
        return cacheEntryListeners.add(cacheEntryListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unregisterCacheEntryListener(CacheEntryListener<?, ?> cacheEntryListener) {
        return cacheEntryListeners.remove(cacheEntryListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invokeEntryProcessor(K key, EntryProcessor<K, V> entryProcessor) {
        checkStatusStarted();
        if (key == null) {
            throw new NullPointerException();
        }
        if (key == entryProcessor) {
            throw new NullPointerException();
        }
        Object result = null;
        lockManager.lock(key);
        try {
            RIMutableEntry<K, V> entry = new RIMutableEntry<K, V>(key, store);
            result = entryProcessor.process(entry);
            entry.commit();
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
        return new RIEntryIterator<K, V>(store.iterator(), lockManager);
    }

    @Override
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
        super.stop();
        store.removeAll();
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

    private V getInternal(K key) {
        long start = statisticsEnabled() ? System.nanoTime() : 0;

        V value = null;
        lockManager.lock(key);
        try {
            value = store.get(key);
        } finally {
            lockManager.unLock(key);
        }
        if (statisticsEnabled()) {
            statistics.addGetTimeNano(System.nanoTime() - start);
        }
        if (value == null) {
            if (statisticsEnabled()) {
                statistics.increaseCacheMisses(1);
            }
            if (getCacheLoader() != null) {
                return getFromLoader(key);
            } else {
                return null;
            }
        } else {
            if (statisticsEnabled()) {
                statistics.increaseCacheHits(1);
            }
            return value;
        }
    }

    private V getFromLoader(K key) {
        Entry<K, ? extends V> entry = getCacheLoader().load(key);
        if (entry != null) {
            store.put(entry.getKey(), entry.getValue());
            return entry.getValue();
        } else {
            return null;
        }
    }

    /**
     * Returns the size of the cache.
     *
     * @return the size in entries of the cache
     */
    long getSize() {
        return store.size();
    }

    /**
     * {@inheritDoc}
     *
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
     * {@inheritDoc}
     * TODO: not obvious how iterator should behave with locking.
     * TODO: in the impl below, by the time we get the lock, value may be stale
     *
     * @author Yannis Cosmadopoulos
     */
    private static final class RIEntryIterator<K, V> implements Iterator<Entry<K, V>> {
        private final Iterator<Map.Entry<K, V>> mapIterator;
        private final LockManager<K> lockManager;

        private RIEntryIterator(Iterator<Map.Entry<K, V>> mapIterator, LockManager<K> lockManager) {
            this.mapIterator = mapIterator;
            this.lockManager = lockManager;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return mapIterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry<K, V> next() {
            Map.Entry<K, V> mapEntry = mapIterator.next();
            K key = mapEntry.getKey();
            lockManager.lock(key);
            try {
                return new RIEntry<K, V>(key, mapEntry.getValue());
            } finally {
                lockManager.unLock(key);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
        private final CacheLoader<K, ? extends V> cacheLoader;
        private final K key;

        RICacheLoaderLoadCallable(RICache<K, V> cache, CacheLoader<K, ? extends V> cacheLoader, K key) {
            this.cache = cache;
            this.cacheLoader = cacheLoader;
            this.key = key;
        }

        @Override
        public V call() throws Exception {
            Entry<K, ? extends V> entry = cacheLoader.load(key);
            cache.put(entry.getKey(), entry.getValue());
            return entry.getValue();
        }
    }

    /**
     * Callable used for cache loader.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @author Yannis Cosmadopoulos
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
     * Simple lock management
     *
     * @param <K> the type of the object to be locked
     * @author Yannis Cosmadopoulos
     * @since 1.0
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
         * Factory/pool
         *
         * @author Yannis Cosmadopoulos
         * @since 1.0
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
     * A mutable entry
     *
     * @param <K>
     * @param <V>
     * @author Yannis Cosmadopoulos
     * @since 1.0
     */
    private static class RIMutableEntry<K, V> implements MutableEntry<K, V> {
        private final K key;
        private V value;
        private final RISimpleCache<K, V> store;
        private boolean exists;
        private boolean remove;

        RIMutableEntry(K key, RISimpleCache<K, V> store) {
            this.key = key;
            this.store = store;
            exists = store.containsKey(key);
        }

        private void commit() {
            if (remove) {
                store.remove(key);
            } else if (value != null) {
                store.put(key, value);
            }
        }

        @Override
        public boolean exists() {
            return exists;
        }

        @Override
        public void remove() {
            remove = true;
            exists = false;
            value = null;
        }

        @Override
        public void setValue(V value) {
            if (value == null) {
                throw new NullPointerException();
            }
            exists = true;
            remove = false;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value != null ? value : store.get(key);
        }
    }
}
