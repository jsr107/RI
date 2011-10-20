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
 * The reference implementation for JSR107.
 * <p/>
 *
 * <p/>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Yannis Cosmadopoulos
 */
class RIByValueSimpleCache<K, V> implements RISimpleCache<K, V> {
    private final Serializer<V> valueSerializer;
    private final Serializer<K> keySerializer;
    private final RIByReferenceSimpleCache<Binary<K>, Binary<V>> store =
        new RIByReferenceSimpleCache<Binary<K>, Binary<V>>();

    /**
     * Constructor
     * @param keySerializer the key serializer
     * @param valueSerializer the value serializer
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
        return returnValue(store.getAndReplace(createKeyHolder(key), createValueHolder(value)));
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
        return returnValue(store.get(createSearchObject(key)));
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
        return returnValue(store.getAndPut(createKeyHolder(key), createValueHolder(value)));
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
    public boolean remove(Object key, V oldValue) {
        return store.remove(createSearchObject(key), createValueHolder(oldValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndRemove(Object key) {
        return returnValue(store.getAndRemove(createSearchObject(key)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        HashMap<Binary<K>, Binary<V>> toStore =
            new HashMap<Binary<K>, Binary<V>>(map.size());
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

    // utilities --------------------------------------------

    private V returnValue(Binary<V> binary) {
        return binary == null ? null : binary.get();
    }

    private Binary<V> createValueHolder(V value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return valueSerializer.createBinary(value);
    }

    private Binary<K> createKeyHolder(K key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return keySerializer.createBinary(key);
    }

    private Object createSearchObject(Object o) {
        return new SearchObject(o);
    }

    /**
     * Wrapped object for search
     */
    private static final class SearchObject {
        private final Object searchObject;

        private SearchObject(Object searchObject) {
            this.searchObject = searchObject;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof Binary)) return false;

            Binary<?> that = (Binary<?>) o;

            return searchObject.hashCode() == that.hashCode() && searchObject.equals(that.get());
        }

        @Override
        public int hashCode() {
            return searchObject.hashCode();
        }
    }

    /**
     * Wrapped iterator
     *
     * @param <K> key type
     * @param <V> value type
     */
    private static final class WrappedIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        private final Iterator<Map.Entry<Binary<K>, Binary<V>>> iterator;

        private WrappedIterator(Iterator<Map.Entry<Binary<K>, Binary<V>>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            Map.Entry<Binary<K>, Binary<V>> next = iterator.next();
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
        private static final class WrappedEntry<K, V> implements Map.Entry<K, V> {
            private final Map.Entry<Binary<K>, Binary<V>> entry;

            private WrappedEntry(Map.Entry<Binary<K>, Binary<V>> entry) {
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
}
