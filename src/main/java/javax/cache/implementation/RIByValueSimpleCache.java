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

import javax.cache.Serializer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The reference implementation for JSR107.
 * <p/>
 * This is meant to act as a proof of concept for the API. It is not threadsafe or high performance. It therefore is
 * not suitable for use in production. Please use a production implementation of the API.
 * <p/>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Yannis Cosmadopoulos
 */
class RIByValueSimpleCache<K, V> implements RISimpleCache<K, V> {
    private final Serializer<V> valueSerializer;
    private final Serializer<K> keySerializer;
    private final RIByReferenceSimpleCache<Serializer.Binary<K>, Serializer.Binary<V>> store =
        new RIByReferenceSimpleCache<Serializer.Binary<K>, Serializer.Binary<V>>();

    /**
     * Constructor
     */
    public RIByValueSimpleCache(Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putIfAbsent(K key, V value) {
        return store.putIfAbsent(createKeyHolder(key), createValueHolder(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return store.replace(createKeyHolder(key), createValueHolder(oldValue), createValueHolder(newValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V value) {
        return store.replace(createKeyHolder(key), createValueHolder(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndReplace(K key, V value) {
        Serializer.Binary<V> binary = store.getAndReplace(createKeyHolder(key), createValueHolder(value));
        return binary == null ? null : binary.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return store.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        return store.containsKey(createSearchObject(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(Object key) {
        Serializer.Binary<V> binary = store.get(createSearchObject(key));
        return binary == null ? null : binary.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(K key, V value) {
        store.put(createKeyHolder(key), createValueHolder(value));
    }

    @Override
    public V getAndPut(K key, V value) {
        Serializer.Binary<V> binary = store.getAndPut(createKeyHolder(key), createValueHolder(value));
        return binary == null ? null : binary.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object key) {
        return store.remove(createSearchObject(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndRemove(Object key) {
        Serializer.Binary<V> binary = store.getAndRemove(createSearchObject(key));
        return binary == null ? null : binary.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        HashMap<Serializer.Binary<K>, Serializer.Binary<V>> toStore =
            new HashMap<Serializer.Binary<K>, Serializer.Binary<V>>(map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            toStore.put(createKeyHolder(entry.getKey()),
                createValueHolder(entry.getValue()));
        }
        store.putAll(toStore);
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
        return new WrappedIterator<K, V>(store.iterator());
    }

    /**
     * Wrapped iterator
     *
     * @param <K> key type
     * @param <V> value type
     */
    private static final class WrappedIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        private final Iterator<Map.Entry<Serializer.Binary<K>, Serializer.Binary<V>>> iterator;

        public WrappedIterator(Iterator<Map.Entry<Serializer.Binary<K>, Serializer.Binary<V>>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            Map.Entry<Serializer.Binary<K>, Serializer.Binary<V>> next = iterator.next();
            return new WrappedEntry<K, V>(next);
        }

        @Override
        public void remove() {
            iterator.remove();
        }

        /**
         * A wrapped entry
         *
         * @param <K> key type
         * @param <V> value type
         */
        private static class WrappedEntry<K, V> implements Map.Entry<K, V> {
            private final Map.Entry<Serializer.Binary<K>, Serializer.Binary<V>> entry;

            public WrappedEntry(Map.Entry<Serializer.Binary<K>, Serializer.Binary<V>> entry) {
                this.entry = entry;
            }

            @Override
            public K getKey() {
                return entry.getKey().get();
            }

            @Override
            public V getValue() {
                return entry.getValue().get();
            }

            @Override
            public V setValue(V v) {
                throw new UnsupportedOperationException();
            }
        }
    }

    // utilities --------------------------------------------

    private Serializer.Binary<V> createValueHolder(V value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return valueSerializer.createBinary(value);
    }

    private Serializer.Binary<K> createKeyHolder(K key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return keySerializer.createBinary(key);
    }

    private Object createSearchObject(final Object o) {
        return new Object() {
            @Override
            public boolean equals(Object o1) {
                if (this == o1) return true;
                if (o1 == null || !(o1 instanceof Serializer.Binary)) return false;

                Serializer.Binary that = (Serializer.Binary) o1;

                return o.hashCode() == that.hashCode() && o.equals(that.get());
            }

            @Override
            public int hashCode() {
                return o.hashCode();
            }
        };
    }
}
