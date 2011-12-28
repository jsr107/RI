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

import javax.cache.Cache;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResult;


/**
 * Interceptor for {@link CacheResult}
 *
 * @author Rick Hightower
 * @author Eric Dalquist
 * @param <I> The intercepted method invocation
 * @since 1.0
 */
public abstract class AbstractCacheResultInterceptor<I> extends AbstractKeyedCacheInterceptor<I, CacheResultMethodDetails> {
 
    /**
     * Handles the {@link Cache#get(Object)} and {@link Cache#put(Object, Object)} logic as specified for the
     * {@link CacheResult} annotation
     * 
     * @param cacheContextSource The intercepted invocation
     * @param invocation The intercepted invocation
     * @return The result from {@link #proceed(Object)}
     * @throws Throwable if {@link #proceed(Object)} threw
     */
    public final Object cacheResult(CacheContextSource<I> cacheContextSource, I invocation) throws Throwable {
        //Load details about the annotated method
        final InternalCacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext = 
                cacheContextSource.getCacheKeyInvocationContext(invocation);
        final CacheResultMethodDetails methodDetails = 
                this.getStaticCacheKeyInvocationContext(cacheKeyInvocationContext, InterceptorType.CACHE_RESULT);
        
        //Resolve primary cache
        final CacheResolver cacheResolver = methodDetails.getCacheResolver();
        final Cache<Object, Object> cache = cacheResolver.resolveCache(cacheKeyInvocationContext);
        
        //Resolve exception cache
        final Cache<Object, Throwable> exceptionCache; 
        final CacheResolver exceptionCacheResolver = methodDetails.getExceptionCacheResolver();
        if (exceptionCacheResolver != null) {
            exceptionCache = exceptionCacheResolver.resolveCache(cacheKeyInvocationContext);
        }
        else {
            exceptionCache = null;
        }

        //Generate the cache key
        final CacheKeyGenerator cacheKeyGenerator = methodDetails.getCacheKeyGenerator();
        final CacheKey cacheKey = cacheKeyGenerator.generateCacheKey(cacheKeyInvocationContext);
        
        final CacheResult cacheResultAnnotation = methodDetails.getCacheAnnotation();
        
        //If skip-get is false check for a cached result or a cached exception
        Object result;
        if (!cacheResultAnnotation.skipGet()) {
            //Look in cache for existing data
            result = cache.get(cacheKey);
            if (result != null) {
                //Cache hit, return result
                return result;
            }
            
            //Look for a cached exception
            if (exceptionCache != null) {
                final Throwable throwable = exceptionCache.get(cacheKey);
                if (throwable != null) {
                    //Found exception, re-throw
                    throw throwable;
                }
            }
        }
        
        try {
            //Call the annotated method
            result = this.proceed(invocation);
        
            //Cache non-null result
            if (result != null) {
                cache.put(cacheKey, result);
            }

            return result;
        }
        catch (Throwable t) {
            //If exception caching is enabled check if the throwable passes the include/exclude filters and then cache it
            if (exceptionCache != null) {
                final Class<? extends Throwable>[] cachedExceptions = cacheResultAnnotation.cachedExceptions();
                final Class<? extends Throwable>[] nonCachedExceptions = cacheResultAnnotation.nonCachedExceptions();
                final boolean included = ClassFilter.isIncluded(t, cachedExceptions, nonCachedExceptions, true);
                if (included) {
                    //Cache the exception for future rethrow
                    exceptionCache.put(cacheKey, t);
                }
            }

            throw t;
        }
    }
}
