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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;

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
    private final HashMap<K, V> store = new HashMap<K, V>();
    private final CacheConfiguration configuration;
    private final boolean ignoreNullKeyOnRead;
    private final boolean allowNullValue;
    private volatile Status status;
    private final Set<ScopedListener> cacheEntryListeners = new CopyOnWriteArraySet<ScopedListener>();


    private RICache(CacheConfiguration configuration, boolean ignoreNullKeyOnRead, boolean allowNullValue) {
        status = Status.UNITIALISED;
        assert configuration != null;
        this.configuration = new UnmodifiableCacheConfiguration(configuration);
        this.ignoreNullKeyOnRead = ignoreNullKeyOnRead;
        this.allowNullValue = allowNullValue;
    }

    /**
     * {@inheritDoc}
     */
    public V get(Object key) throws CacheException {
        checkStatusStarted();
        if (key == null) {
            if (ignoreNullKeyOnRead) {
                return null;
            } else {
                throw new NullPointerException("key");
            }
        } else {
            //noinspection SuspiciousMethodCalls
            return store.get(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<K, V> getAll(Collection<? extends K> keys) {
        checkStatusStarted();
        // will throw NPE if keys=null
        HashMap<K, V> map = new HashMap<K, V>(keys.size());
        for (K key : keys) {
            if (key == null) {
                if (!ignoreNullKeyOnRead) {
                    throw new NullPointerException();
                }
            } else {
                map.put(key, store.get(key));
            }
        }
        return map;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        checkStatusStarted();
        if (key == null) {
            if (ignoreNullKeyOnRead) {
                return false;
            } else {
                throw new NullPointerException("key");
            }
        } else {
            //noinspection SuspiciousMethodCalls
            return store.containsKey(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Future load(K key, CacheLoader<K, V> specificLoader, Object loaderArgument) {
        checkStatusStarted();
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Future loadAll(Collection<? extends K> keys, CacheLoader specificLoader, Object loaderArgument) {
        checkStatusStarted();
        throw new UnsupportedOperationException();
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
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (!allowNullValue && value == null) {
            throw new NullPointerException("value");
        } else {
            store.put(key, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends K, ? extends V> map) {
        checkStatusStarted();
        if (map == null) {
            throw new NullPointerException();
        }
        if (!ignoreNullKeyOnRead) {
            if (map.containsKey(null)) {
                throw new NullPointerException("key");
            }
        }
        if (!allowNullValue) {
            if (map.containsValue(null)) {
                throw new NullPointerException("key");
            }
        }
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                store.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putIfAbsent(K key, V value) {
        checkStatusStarted();
        if (key == null && !ignoreNullKeyOnRead) {
            throw new NullPointerException("key");
        }
        if (value == null && !allowNullValue) {
            throw new NullPointerException("value");
        }
        if (key != null && !store.containsKey(key)) {
            store.put(key, value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key) {
        checkStatusStarted();
        if (key == null) {
            if (ignoreNullKeyOnRead) {
                return false;
            } else {
                throw new NullPointerException();
            }
        } else {
            //noinspection SuspiciousMethodCalls
            return (store.remove(key) != null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public V getAndRemove(Object key) {
        checkStatusStarted();
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(K key, V oldValue, V newValue) {
        checkStatusStarted();
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (!allowNullValue && (oldValue == null || newValue == null)) {
            throw new NullPointerException("value");
        }
        if (store.containsKey(key)) {
            V old = store.get(key);
            boolean replace = (old == null) ? (oldValue == null) : old.equals(oldValue);
            if (replace) {
                store.put(key, newValue);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(K key, V value) {
        checkStatusStarted();
        return getAndReplace(key, value) != null;
    }

    /**
     * {@inheritDoc}
     */
    public V getAndReplace(K key, V value) {
        checkStatusStarted();
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (!allowNullValue && value == null) {
            throw new NullPointerException("value");
        }
        if (store.containsKey(key)) {
            V old = store.get(key);
            store.put(key, value);
            return old;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(Collection<? extends K> keys) {
        checkStatusStarted();
        if (keys == null) {
            throw new NullPointerException("keys");
        }
        if (!ignoreNullKeyOnRead && keys.contains(null)) {
            throw new NullPointerException();
        }
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
    public void initialise() throws CacheException {
        status = Status.STARTED;
    }

    /**
     * {@inheritDoc}
     */
    public void stopAndDispose() throws CacheException {
        status = Status.STOPPING;
        store.clear();
        status = Status.STOPPED;
    }

    private void checkStatusStarted() {
        if (!status.equals(Status.STARTED))
            throw new IllegalStateException("The cache status is not STARTED");
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return status;
    }

    /**
     * An unmodifiable version of CacheConfiguration. This cache does not support dynamoc modification
     * of configuration.
     * {@inheritDoc}
     * @author Yannis Cosmadopoulos
     */
    private static class UnmodifiableCacheConfiguration implements CacheConfiguration {
        private final CacheConfiguration config;

        UnmodifiableCacheConfiguration(CacheConfiguration config) {
            this.config = config;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isReadThrough() {
            return config.isReadThrough();
        }

        /**
         * {@inheritDoc}
         */
        public void setReadThrough(boolean readThrough) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isWriteThrough() {
            return config.isWriteThrough();
        }

        /**
         * {@inheritDoc}
         */
        public void setWriteThrough(boolean writeThrough) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isStoreByValue() {
            return config.isStoreByValue();
        }

        /**
         * {@inheritDoc}
         */
        public void setStoreByValue(boolean storeByValue) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A Builder for RICache
     * @param <K>
     * @param <V>
     * @author Yannis Cosmadopoulos
     */
    public static class Builder<K, V> {
        private CacheConfiguration configuration;
        private boolean ignoreNullKeyOnRead = true;
        private boolean allowNullValue = true;

        /**
         * Builds the cache
         * @return a constructed cache.
         */
        public RICache<K, V> build() {
            if (configuration == null) {
                configuration = new RICacheConfiguration.Builder().build();
            }
            return new RICache<K, V>(configuration, ignoreNullKeyOnRead, allowNullValue);
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
         * @param ignoreNullKeyOnRead the value of the flag
         * @return the builder
         */
        public Builder<K, V> setIgnoreNullKeyOnRead(boolean ignoreNullKeyOnRead) {
            this.ignoreNullKeyOnRead = ignoreNullKeyOnRead;
            return this;
        }

        /**
         *
         * @param allowNullValue the value of the flag
         * @return the builder
         */
        public Builder<K, V> setAllowNullValue(boolean allowNullValue) {
            this.allowNullValue = allowNullValue;
            return this;
        }
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

            return  (this.getKey() == null ? e2.getKey() == null : this.getKey().equals(e2.getKey())) &&
                    (this.getValue() == null ? e2.getValue() == null : this.getValue().equals(e2.getValue()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return (getKey() == null ? 0 :
                    getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode());
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
}
