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

import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;

/**
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 * @author Greg Luck
 * @since 1.0
 */
public class RICacheEntryEvent<K, V> extends CacheEntryEvent<K, V> {

    private K key;
    private V value;
    private V oldValue;
    private boolean oldValueAvailable;

    /**
     * Constructs a cache entry event from a given cache as source
     *
     * @param source the cache that originated the event
     */
    public RICacheEntryEvent(Cache source, K key, V value) {
        super(source);
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the key of the cache entry with the event
     *
     * @return the key
     */
    @Override
    public K getKey() {
        return key;
    }

    /**
     * Returns the value of the cache entry with the event
     *
     * @return the value
     */
    @Override
    public V getValue() {
        return value;
    }

    /**
     * Returns the value of the cache entry with the event
     *
     * @return the value
     * @throws UnsupportedOperationException if the old value is not available
     */
    @Override
    public V getOldValue() throws UnsupportedOperationException {
        if (isOldValueAvailable()) {
            return oldValue;
        } else {
            throw new UnsupportedOperationException("Old value is not available for key");
        }
    }

    /**
     * Whether the old value is available
     *
     * @return true if the old value is populated
     */
    @Override
    public boolean isOldValueAvailable() {
        return oldValueAvailable;
    }
}
