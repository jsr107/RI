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


import javax.cache.Cache;
import javax.cache.interceptor.CacheInvocationParameter;
import javax.cache.interceptor.CacheKey;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CachePut;
import javax.cache.interceptor.CacheResolver;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;


/**
 * Interceptor for {@link CachePut}
 * 
 * @author Rick Hightower
 * @author Eric Dalquist
 */
@CachePut @Interceptor
public class RICachePutInterceptor extends BaseKeyedCacheInterceptor<CachePutMethodDetails> {
    
    @Inject
    private RICacheLookupUtil lookup;
    
 
    /**
     * @param invocationContext The intercepted invocation
     * @return The result from {@link InvocationContext#proceed()}
     * @throws Exception likely {@link InvocationContext#proceed()} threw an exception
     */
    @AroundInvoke
    public Object cachePut(InvocationContext invocationContext) throws Exception {
        final CacheKeyInvocationContextImpl cacheKeyInvocationContext = this.lookup.getCacheKeyInvocationContext(invocationContext);
        final CachePutMethodDetails methodDetails = this.getStaticCacheKeyInvocationContext(cacheKeyInvocationContext, InterceptorType.CACHE_PUT);
        
        final CachePut cachePutAnnotation = methodDetails.getCacheAnnotation();
        final boolean afterInvocation = cachePutAnnotation.afterInvocation();
        
        final CacheInvocationParameter valueParameter = cacheKeyInvocationContext.getValueParameter();
        final Object value = valueParameter.getValue();
        
        if (!afterInvocation) {
            cacheValue(cacheKeyInvocationContext, methodDetails, value);
        }
        
        //Call the annotated method
        final Object result = invocationContext.proceed();
        
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
    protected void cacheValue(final CacheKeyInvocationContextImpl cacheKeyInvocationContext,
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
