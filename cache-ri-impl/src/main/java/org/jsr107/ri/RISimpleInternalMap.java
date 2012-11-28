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
 * A simple implementation of a {@link RIInternalMap} based on a {@link ConcurrentHashMap}.
 * 
 * @param <K> the type of keys stored
 * @param <V> the type of values stored
 * 
 * @author Brian Oliver
 */
class RISimpleInternalMap<K, V> implements RIInternalMap<K, V> {

    /**
     * The map containing the entries.
     */
    private final ConcurrentHashMap<K, V> internalMap = new ConcurrentHashMap<K, V>();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        //noinspection SuspiciousMethodCalls
        return internalMap.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(K key, V value) {
        internalMap.put(key, value);
    }

    @Override
    public V getAndPut(K key, V value) {
        return internalMap.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        internalMap.putAll(map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object key) {
        //noinspection SuspiciousMethodCalls
        return internalMap.remove(key) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return internalMap.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() {
        internalMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return internalMap.entrySet().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(Object key) {
        //noinspection SuspiciousMethodCalls
        return internalMap.get(key);
    }
}
