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


import javax.cache.Cache;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResolver;


/**
 * Interceptor for {@link CachePut}
 * 
 * @author Rick Hightower
 * @author Eric Dalquist
 * @param <I> The intercepted method invocation
 */
public abstract class AbstractCachePutInterceptor<I> extends AbstractKeyedCacheInterceptor<I, CachePutMethodDetails> {
    
    /**
     * Handles the {@link Cache#put(Object, Object)} as specified for the {@link CachePut} annotation
     * 
     * @param cacheContextSource The intercepted invocation
     * @param invocation The intercepted invocation
     * @return The result from {@link #proceed(Object)}
     * @throws Throwable if {@link #proceed(Object)} threw
     */
    public Object cachePut(CacheContextSource<I> cacheContextSource, I invocation) throws Throwable {
        final AbstractCacheKeyInvocationContextImpl<I> cacheKeyInvocationContext = cacheContextSource.getCacheKeyInvocationContext(invocation);
        final CachePutMethodDetails methodDetails = this.getStaticCacheKeyInvocationContext(cacheKeyInvocationContext, InterceptorType.CACHE_PUT);
        
        final CachePut cachePutAnnotation = methodDetails.getCacheAnnotation();
        final boolean afterInvocation = cachePutAnnotation.afterInvocation();
        
        final CacheInvocationParameter valueParameter = cacheKeyInvocationContext.getValueParameter();
        final Object value = valueParameter.getValue();
        
        if (!afterInvocation) {
            cacheValue(cacheKeyInvocationContext, methodDetails, value);
        }
        
        //Call the annotated method
        final Object result = this.proceed(invocation);
        
        if (afterInvocation) {
            cacheValue(cacheKeyInvocationContext, methodDetails, value);
        }
        
        return result;
    }


    /**
     * Lookup the Cache, generate a CacheKey and store the value in the cache.
     * 
     * @param cacheKeyInvocationContext The invocation context 
     * @param methodDetails The details about the cached method
     * @param value The value to cache
     */
    protected void cacheValue(final AbstractCacheKeyInvocationContextImpl<I> cacheKeyInvocationContext,
            final CachePutMethodDetails methodDetails, final Object value) {
        
        //Ignore null values
        if (value == null) {
            return;
        }
        
        final CacheResolver cacheResolver = methodDetails.getCacheResolver();
        final Cache<Object, Object> cache = cacheResolver.resolveCache(cacheKeyInvocationContext);

        final CacheKeyGenerator cacheKeyGenerator = methodDetails.getCacheKeyGenerator();
        final CacheKey cacheKey = cacheKeyGenerator.generateCacheKey(cacheKeyInvocationContext);
        
        cache.put(cacheKey, value);
    }
}
