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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is NOT thread safe
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public class RIExpiringByReferenceSimpleCache<K, V> implements RISimpleCache<K, V> {
    private final RIByReferenceSimpleCache<K, ExpiryWrapper<V>> store =
            new RIByReferenceSimpleCache<K, ExpiryWrapper<V>>();

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(Object key) {
        ExpiryWrapper<V> value = store.get(key);
        if (value == null) {
            return null;
        } else {
            long now = System.currentTimeMillis();
            return value.getValue(now);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        ExpiryWrapper<V> value = store.get(key);
        return value != null;
//        if (value == null) {
//            return false;
//        } else {
//            return true;
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(K key, V value) {
        long now = System.currentTimeMillis();
        store.put(key, new ExpiryWrapper<V>(value, now));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndPut(K key, V value) {
        long now = System.currentTimeMillis();
        ExpiryWrapper<V> oldValue = store.getAndPut(key, new ExpiryWrapper<V>(value, now));
        if (oldValue == null) {
            return null;
        } else {
            return oldValue.getValue(now);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        long now = System.currentTimeMillis();
        Map<K, ExpiryWrapper<V>> toStore =
                new HashMap<K, ExpiryWrapper<V>>(map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            toStore.put(entry.getKey(), new ExpiryWrapper<V>(entry.getValue(), now));
        }
        store.putAll(toStore);
    }

    /**
     * Not atomic.
     * {@inheritDoc}
     */
    @Override
    public boolean putIfAbsent(K key, V value) {
        ExpiryWrapper<V> oldValue = store.get(key);
        long now = System.currentTimeMillis();
        if (oldValue == null) {
            store.put(key, new ExpiryWrapper<V>(value, now));
            return false;
        } else {
            return store.putIfAbsent(key, new ExpiryWrapper<V>(value, now));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object key) {
        return getAndRemove(key) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object key, V oldValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndRemove(Object key) {
        ExpiryWrapper<V> oldValue = store.getAndRemove(key);
        if (oldValue == null) {
            return null;
        } else {
            long now = System.currentTimeMillis();
            return oldValue.getValue(now);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndReplace(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() {
        store.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * A fine wrapper
     * @param <V> the value
     */
    private static class ExpiryWrapper<V> {
        private V value;
        private long modificationTime;
        private long accessTime;

        ExpiryWrapper(V value, long creationTime) {
            if (value == null) {
                throw new NullPointerException();
            }
            this.value = value;
            this.modificationTime = creationTime;
            this.accessTime = creationTime;
        }

        public V getValue(long accessTime) {
            this.accessTime = accessTime;
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof ExpiryWrapper) {
                ExpiryWrapper<?> that = (ExpiryWrapper<?>) o;
                return value.equals(that.value);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
