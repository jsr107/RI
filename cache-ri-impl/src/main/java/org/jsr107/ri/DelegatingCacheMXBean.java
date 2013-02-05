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
import javax.cache.Status;
import javax.cache.mbeans.CacheMXBean;

/**
 * Class to help implementers
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public class DelegatingCacheMXBean<K, V> implements CacheMXBean {
    private final Cache<K, V> cache;

    /**
     * Constructor
     * @param cache the cache
     */
    public DelegatingCacheMXBean(Cache<K, V> cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return cache.getName();
    }

    @Override
    public Status getStatus() {
        return cache.getStatus();
    }

    private String mbeanSafe(String string) {
        return string == null ? "" : string.replaceAll(":|=|\n", ".");
    }
}
