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

package javax.cache.annotation.impl;

import java.lang.annotation.Annotation;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheBuilder;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;

/**
 * Default {@link CacheResolverFactory} that uses the default {@link CacheManager} and finds the {@link Cache}
 * using {@link CacheManager#getCache(String)} or {@link CacheManager#createCacheBuilder(String)}} if the
 * named {@link Cache} doesn't exist. Returns a {@link DefaultCacheResolver} that wraps the found
 * {@link Cache} 
 *
 * @author Eric Dalquist
 * @author Rick Hightower
 * @since 1.0
 */
public class DefaultCacheResolverFactory implements CacheResolverFactory {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private final CacheManager cacheManager;

    /**
     * Constructs the resolver
     * @param cacheManager the cache manager to use
     */
    public DefaultCacheResolverFactory(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Constructs the resolver
     */
    public DefaultCacheResolverFactory() {
        this.cacheManager = Caching.getCacheManager();
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.CacheResolverFactory#getCacheResolver(javax.cache.annotation.CacheMethodDetails)
     */
    @Override
    public CacheResolver getCacheResolver(CacheMethodDetails<? extends Annotation> cacheMethodDetails) {
        final String cacheName = cacheMethodDetails.getCacheName();
        Cache<Object, Object> cache = this.cacheManager.getCache(cacheName);
        
        if (cache == null) {
            this.logger.warning("No Cache named '" + cacheName + "' was found in the CacheManager, a copy of the default cache will be created.");
            final CacheBuilder<Object, Object> cacheBuilder = this.cacheManager.<Object, Object>createCacheBuilder(cacheName);
            cache = cacheBuilder.build();
        }
        
        return new DefaultCacheResolver(cache);
    }
    
    @Override
    public CacheResolver getExceptionCacheResolver(CacheMethodDetails<CacheResult> cacheMethodDetails) {
        final CacheResult cacheResultAnnotation = cacheMethodDetails.getCacheAnnotation();
        final String exceptionCacheName = cacheResultAnnotation.exceptionCacheName();
        if (exceptionCacheName == null || exceptionCacheName.trim().length() == 0) {
            throw new IllegalArgumentException("Can only be called when CacheResult.exceptionCacheName() is specified");
        }
        
        Cache<Object, Object> cache = this.cacheManager.getCache(exceptionCacheName);
        
        if (cache == null) {
            this.logger.warning("No Cache named '" + exceptionCacheName + 
                    "' was found in the CacheManager, a copy of the default cache will be created.");
            final CacheBuilder<Object, Object> cacheBuilder = this.cacheManager.<Object, Object>createCacheBuilder(exceptionCacheName);
            cache = cacheBuilder.build();
        }
        
        return new DefaultCacheResolver(cache);
    }
}
