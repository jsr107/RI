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

package javax.cache.implementation.interceptor;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheBuilder;
import javax.cache.CacheManager;
import javax.cache.CacheManagerFactory;
import javax.cache.interceptor.CacheResolver;

/**
 * Default {@link CacheResolver} that uses the default {@link CacheManager} and finds the {@link Cache}
 * using {@link CacheManager#getCache(String)}, {@link CacheManager#createCacheBuilder(String)}}.
 *
 * @author Eric Dalquist
 * @author Rick Hightower
 * @since 1.0
 */
public class RIDefaultCacheResolver implements CacheResolver {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final CacheManager cacheManager;

    /**
     * Constructs the resolver
     * @param cacheManager the cache manager to use
     */
    public RIDefaultCacheResolver(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Constructs the resolver
     */
    public RIDefaultCacheResolver() {
        this.cacheManager = CacheManagerFactory.INSTANCE.getCacheManager();
    }

    /**
     * @see javax.cache.interceptor.CacheResolver#resolveCache(java.lang.String, java.lang.reflect.Method)
     */
    @Override
    public <K, V> Cache<K, V> resolveCache(String cacheName, Method method) {
        final Cache<K, V> cache = this.cacheManager.getCache(cacheName);
        if (cache != null) {
            return cache;
        }
        
        this.logger.warning("No Cache named '" + cacheName + "' was found in the CacheManager, a copy of the default cache will be created.");
        final CacheBuilder<K, V> cacheBuilder = this.cacheManager.<K, V>createCacheBuilder(cacheName);
        return cacheBuilder.build();
    }

}
