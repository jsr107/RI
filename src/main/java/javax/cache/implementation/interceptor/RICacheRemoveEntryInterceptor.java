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
import javax.cache.interceptor.CacheKey;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheRemoveEntry;
import javax.cache.interceptor.CacheResolver;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;


/**
 * Interceptor for {@link CacheRemoveEntry}
 * 
 * @author Rick Hightower
 * @author Eric Dalquist
 */
@CacheRemoveEntry @Interceptor
public class RICacheRemoveEntryInterceptor extends BaseKeyedCacheInterceptor<CacheRemoveEntryMethodDetails> {
    
    @Inject
    private RICacheLookupUtil lookup;


    /**
     * @param invocationContext The intercepted invocation
     * @return The result from {@link InvocationContext#proceed()}
     * @throws Exception likely {@link InvocationContext#proceed()} threw an exception
     */
    @AroundInvoke
    public Object cacheResult(InvocationContext invocationContext) throws Exception {
        final CacheKeyInvocationContextImpl cacheKeyInvocationContext = this.lookup.getCacheKeyInvocationContext(invocationContext);
        final CacheRemoveEntryMethodDetails methodDetails = 
                this.getStaticCacheKeyInvocationContext(cacheKeyInvocationContext, InterceptorType.CACHE_REMOVE_ENTRY);
        
        final CacheRemoveEntry cacheRemoveEntryAnnotation = methodDetails.getCacheAnnotation();
        final boolean afterInvocation = cacheRemoveEntryAnnotation.afterInvocation();
        
        //If pre-invocation - remove entry
        if (!afterInvocation) {
            cacheRemove(cacheKeyInvocationContext, methodDetails);
        }
        
        //Call the annotated method
        final Object result = invocationContext.proceed();
        
        //If post-invocation - remove entry
        if (afterInvocation) {
            cacheRemove(cacheKeyInvocationContext, methodDetails);
        }
        
        return result;
    }

    /**
     * Remove entry from cache
     * 
     * @param cacheKeyInvocationContext The invocation context 
     * @param methodDetails The details about the cached method
     */
    protected void cacheRemove(final CacheKeyInvocationContextImpl cacheKeyInvocationContext,
            final CacheRemoveEntryMethodDetails methodDetails) {
        
        final CacheResolver cacheResolver = methodDetails.getCacheResolver();
        final Cache<Object, Object> cache = cacheResolver.resolveCache(cacheKeyInvocationContext);

        final CacheKeyGenerator cacheKeyGenerator = methodDetails.getCacheKeyGenerator();
        final CacheKey cacheKey = cacheKeyGenerator.generateCacheKey(cacheKeyInvocationContext);
        
        cache.remove(cacheKey);
    }
}
