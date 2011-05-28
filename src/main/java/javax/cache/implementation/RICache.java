package javax.cache.implementation;

import javax.cache.Cache;
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheStatisticsMBean;
import javax.cache.listeners.CacheEntryListener;
import java.util.*;
import java.util.concurrent.Future;

/**
 * RI
 */
public class RICache<K,V> implements Cache<K,V> {
    private final HashMap<K,V> store = new HashMap<K,V>();
    private final CacheConfiguration configuration;
    private final boolean ignoreNullKeyOnRead;
    private final boolean allowNullValue;

    private RICache(CacheConfiguration configuration, boolean ignoreNullKeyOnRead, boolean allowNullValue) {
        assert configuration != null;
        this.configuration = new UnmodifiableCacheConfiguration(configuration);
        this.ignoreNullKeyOnRead = ignoreNullKeyOnRead;
        this.allowNullValue = allowNullValue;
    }

    /**
     * {@inheritDoc}
     */
    public V get(Object key) throws CacheException {
        if (key == null) {
            if (ignoreNullKeyOnRead) {
                return null;
            } else {
                throw new NullPointerException("key");
            }
        } else {
            return store.get(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<K, V> getAll(Collection<? extends K> keys) {
         // will throw NPE if keys=null
        HashMap<K,V> map = new HashMap<K,V>(keys.size());
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
        if (key == null) {
            if (ignoreNullKeyOnRead) {
                return false;
            } else {
                throw new NullPointerException("key");
            }
        } else {
            return store.containsKey(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Future load(K key, CacheLoader<K,V> specificLoader, Object loaderArgument) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Future loadAll(Collection<? extends K> keys, CacheLoader specificLoader, Object loaderArgument) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public CacheStatisticsMBean getCacheStatistics() {
        //TODO: this satisfies API but maybe we want a real impl?
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void addListener(CacheEntryListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void removeListener(CacheEntryListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void put(K key, V value) {
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
        for (Map.Entry<? extends K, ? extends V> entry: map.entrySet()) {
            if (entry.getKey() != null) {
                store.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putIfAbsent(K key, V value) {
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
        if (key == null) {
            if (ignoreNullKeyOnRead) {
                return false;
            } else {
                throw new NullPointerException();
            }
        } else {
            return (store.remove(key) != null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public V getAndRemove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(K key, V oldValue, V newValue) {
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
        return getAndReplace(key, value) != null;
    }

    /**
     * {@inheritDoc}
     */
    public V getAndReplace(K key, V value) {
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
    public Iterator<Entry<K, V>> iterator() {
        throw new UnsupportedOperationException();
    }

    private static class UnmodifiableCacheConfiguration implements CacheConfiguration {
        private final CacheConfiguration config;

        UnmodifiableCacheConfiguration(CacheConfiguration config) {
            this.config = config;
        }

        public boolean isReadThrough() {
            return config.isReadThrough();
        }

        public void setReadThrough(boolean readThrough) {
            throw new UnsupportedOperationException();
        }

        public boolean isWriteThrough() {
            return config.isWriteThrough();
        }

        public void setWriteThrough(boolean writeThrough) {
            throw new UnsupportedOperationException();
        }

        public boolean isStoreByValue() {
            return config.isStoreByValue();
        }

        public void setStoreByValue(boolean storeByValue) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Builder<K,V> {
        private CacheConfiguration configuration;
        private boolean ignoreNullKeyOnRead = true;
        private boolean allowNullValue = true;

        public RICache<K,V> build() {
            if (configuration == null) {
                configuration = new RICacheConfiguration.Builder().build();
            }
            return new RICache<K,V>(configuration, ignoreNullKeyOnRead, allowNullValue);
        }

        public Builder<K,V> setCacheConfiguration(CacheConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder<K,V> setIgnoreNullKeyOnRead(boolean ignoreNullKeyOnRead) {
            this.ignoreNullKeyOnRead = ignoreNullKeyOnRead;
            return this;
        }

        public Builder<K,V> setAllowNullValue(boolean allowNullValue) {
            this.allowNullValue = allowNullValue;
            return this;
        }
    }
}
