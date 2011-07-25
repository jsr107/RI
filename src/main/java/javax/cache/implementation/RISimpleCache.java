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

import java.util.Iterator;
import java.util.Map;

/**
 * The reference implementation for JSR107.
 * <p/>
 * This is an adaptation of {@link java.util.concurrent.ConcurrentMap} to meet the needs
 * of a potentially distributed cache. It is a strict subset of the {@link javax.cache.Cache}
 * interface.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 * @author Yannis Cosmadopoulos
 */
interface RISimpleCache<K, V> {

    /**
     * @param key the key
     * @return the value
     * @see javax.cache.Cache#get(Object)
     */
    V get(Object key);

    /**
     * @param key the key
     * @return true if exists
     * @see javax.cache.Cache#containsKey(Object)
     */
    boolean containsKey(Object key);

    /**
     * @param key the key
     * @param value the valu
     * @see javax.cache.Cache#put(Object, Object)
     */
    void put(K key, V value);

    /**
     * @param key the key
     * @param value the value
     * @return the old value
     * @see javax.cache.Cache#getAndPut(Object, Object)
     */
    V getAndPut(K key, V value);

    /**
     * @param map the map of key/values
     * @see javax.cache.Cache#putAll(java.util.Map)
     */
    void putAll(Map<? extends K, ? extends V> map);

    /**
     * @param key the key
     * @param value the value
     * @return true if replace happened
     * @see javax.cache.Cache#putIfAbsent(Object, Object)
     */
    boolean putIfAbsent(K key, V value);

    /**
     * @param key the key
     * @return true if removed
     * @see javax.cache.Cache#remove(Object)
     */
    boolean remove(Object key);

    /**
     * @param key the key
     * @return the previous value
     * @see javax.cache.Cache#remove(Object)
     */
    V getAndRemove(Object key);

    /**
     * @param key the key
     * @param oldValue old value
     * @param newValue new value
     * @return whether replace happened
     * @see javax.cache.Cache#replace(Object, Object, Object)
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * @param key the key
     * @param value the value
     * @return whether replaced
     * @see javax.cache.Cache#replace(Object, Object)
     */
    boolean replace(K key, V value);

    /**
     * @param key the key
     * @param value the new value
     * @return the old value
     * @see javax.cache.Cache#replace(Object, Object)
     */
    V getAndReplace(K key, V value);

    /**
     * @see java.util.Map#size()
     * @return the size
     */
    int size();

    /**
     * @see javax.cache.Cache#removeAll()
     */
    void removeAll();

    /**
     * @return the iterator
     * @see javax.cache.Cache#iterator()
     */
    Iterator<Map.Entry<K, V>> iterator();
}
