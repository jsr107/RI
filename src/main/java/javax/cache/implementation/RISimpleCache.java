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
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Yannis Cosmadopoulos
 */
interface RISimpleCache<K, V> {
    /**
     * @see javax.cache.Cache#containsKey(Object)
     */
    boolean containsKey(Object key);

    /**
     * @see javax.cache.Cache#put(Object, Object)
     */
    void put(K key, V value);

    /**
     * @see javax.cache.Cache#putAll(java.util.Map)
     */
    void putAll(Map<? extends K, ? extends V> map);

    /**
     * @see javax.cache.Cache#putIfAbsent(Object, Object)
     */
    boolean putIfAbsent(K key, V value);

    /**
     * @see javax.cache.Cache#remove(Object)
     */
    boolean remove(Object key);

    /**
     * @see javax.cache.Cache#remove(Object)
     */
    V getAndRemove(Object key);

    /**
     * @see javax.cache.Cache#replace(Object, Object, Object)
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * @see javax.cache.Cache#replace(Object, Object)
     */
    boolean replace(K key, V value);

    /**
     * @see javax.cache.Cache#replace(Object, Object)
     */
    V getAndReplace(K key, V value);

    /**
     * @see javax.cache.Cache
     */
    int size();

    /**
     * @see javax.cache.Cache#removeAll()
     */
    void removeAll();

    /**
     * @see javax.cache.Cache#iterator()
     */
    Iterator<Map.Entry<K, V>> iterator();

    /**
     * @see javax.cache.Cache#get(Object)
     */
    V get(Object key);
}
