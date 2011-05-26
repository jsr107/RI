package javax.cache.implementation;

import javax.cache.Cache;
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheStatisticsMBean;
import javax.cache.listeners.CacheEntryListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * RI
 */
public class RICache<K,V> implements Cache<K,V> {
    private final ConcurrentHashMap<K,V> store = new ConcurrentHashMap<K,V>();
    private final CacheConfiguration configuration;

    private RICache(CacheConfiguration configuration) {
        assert configuration != null;
        this.configuration = new UnmodifiableCacheConfiguration(configuration);
    }

    /**
     * {@inheritDoc}
     */
    public V get(Object key) throws CacheException {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return store.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Map<K, V> getAll(Collection<? extends K> keys) {
         // will throw NPE if keys=null
        HashMap<K,V> map = new HashMap<K,V>(keys.size());
        for (K key : keys) {
            map.put(key, store.get(key));
        }
        return map;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return store.containsKey(key);
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
    public Entry<K,V> getCacheEntry(Object key) {
        V value = store.get(key);
        return value == null ? null : new RIEntry<K,V>((K) key, value);
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
        if (value == null) {
            throw new NullPointerException("value");
        }
        store.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        if (m == null) {
            throw new NullPointerException();
        }
        if (m.containsKey(null)) {
            throw new NullPointerException();
        }
        if (m.containsValue(null)) {
            throw new NullPointerException();
        }
        store.putAll(m);
    }

    /**
     * {@inheritDoc}
     */
    public boolean putIfAbsent(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return (store.remove(key) != null);
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
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public V getAndReplace(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(Collection<? extends K> keys) {
        throw new UnsupportedOperationException();
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

    private static class RIEntry<K,V> implements Entry<K,V> {
        private final K key;
        private final V value;

        private RIEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RIEntry riEntry = (RIEntry) o;

            if (key != null ? !key.equals(riEntry.key) : riEntry.key != null) return false;
            if (value != null ? !value.equals(riEntry.value) : riEntry.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }
    }

    public static class Builder<K,V> {
        private CacheConfiguration configuration;

        public RICache<K,V> build() {
            if (configuration == null) {
                configuration = new RICacheConfiguration.Builder().build();
            }
            return new RICache<K,V>(configuration);
        }

        public Builder<K,V> setCacheConfiguration(CacheConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }
    }
}
