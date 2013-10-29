/**
 *  Copyright 2011-2013 Terracotta, Inc.
 *  Copyright 2011-2013 Oracle America Incorporated
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

package org.jsr107.ri.integration;

import org.jsr107.ri.RICache;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Callable used for cache loader.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 * @author Brian Oliver
 * @author Greg Luck
 */
 class RICacheLoaderLoadAllCallable<K, V> implements Callable<Map<K, ? extends V>> {
  private final RICache<K, V> cache;
  private final CacheLoader<K, ? extends V> cacheLoader;
  private final Collection<? extends K> keys;

  /**
   * Constructor
   */
  RICacheLoaderLoadAllCallable(RICache<K, V> cache, CacheLoader<K, ? extends V> cacheLoader, Collection<? extends K> keys) {
    this.cache = cache;
    this.cacheLoader = cacheLoader;
    this.keys = keys;
  }

  @Override
  public Map<K, ? extends V> call() throws Exception {
    ArrayList<K> keysNotInStore = new ArrayList<K>();
    for (K key : keys) {
      if (!cache.containsKey(key)) {
        keysNotInStore.add(key);
      }
    }
    Map<K, ? extends V> value;
    try {
      value = cacheLoader.loadAll(keysNotInStore);
    } catch (Exception e) {
      if (!(e instanceof CacheLoaderException)) {
        throw new CacheLoaderException("Exception in CacheLoader", e);
      } else {
        throw e;
      }
    }

    cache.putAll(value);
    return value;
  }
}
