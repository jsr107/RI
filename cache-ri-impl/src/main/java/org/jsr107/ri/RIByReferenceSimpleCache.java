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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The reference implementation for JSR107.
 * <p/>
 * A simple adapter implemented in terms of a {@link ConcurrentHashMap}.
 *
 * <p/>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
class RIByReferenceSimpleCache<K, V> implements RISimpleCache<K, V> {
    private final ConcurrentHashMap<K, V> store = new ConcurrentHashMap<K, V>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        //noinspection SuspiciousMethodCalls
        return store.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V put(K key, V value) {
        return store.put(key, value);
    }

    @Override
    public V getAndPut(K key, V value) {
        return store.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        store.putAll(map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putIfAbsent(K key, V value) {
        return store.putIfAbsent(key, value) == null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object key) {
        //noinspection SuspiciousMethodCalls
        return store.remove(key) != null;
    }

    /**
     * @param key      the key
     * @param oldValue the old value to be checked
     * @return true if removed
     * @see javax.cache.Cache#remove(Object)
     */
    @Override
    public boolean remove(Object key, Object oldValue) {
        return store.remove(key, oldValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndRemove(Object key) {
        //noinspection SuspiciousMethodCalls
        return store.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return store.replace(key, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(K key, V value) {
        return store.replace(key, value) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V getAndReplace(K key, V value) {
        return store.replace(key, value);
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
    public void removeAll() {
        store.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return store.entrySet().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(Object key) {
        //noinspection SuspiciousMethodCalls
        return store.get(key);
    }
}
