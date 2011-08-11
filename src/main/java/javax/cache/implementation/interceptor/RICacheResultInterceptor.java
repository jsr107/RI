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
import javax.cache.interceptor.CacheResolver;
import javax.cache.interceptor.CacheResult;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;


/**
 * Interceptor for {@link CacheResult}
 *
 * @author Rick Hightower
 * @author Eric Dalquist
 */
@CacheResult @Interceptor
public class RICacheResultInterceptor extends BaseKeyedCacheInterceptor<CacheResultMethodDetails> {
    
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
        final CacheResultMethodDetails methodDetails = 
                this.getStaticCacheKeyInvocationContext(cacheKeyInvocationContext, InterceptorType.CACHE_RESULT);
        
        final CacheResolver cacheResolver = methodDetails.getCacheResolver();
        final Cache<Object, Object> cache = cacheResolver.resolveCache(cacheKeyInvocationContext);

        final CacheKeyGenerator cacheKeyGenerator = methodDetails.getCacheKeyGenerator();
        final CacheKey cacheKey = cacheKeyGenerator.generateCacheKey(cacheKeyInvocationContext);
        
        final CacheResult cacheResultAnnotation = methodDetails.getCacheAnnotation();
        
        Object result;
        if (!cacheResultAnnotation.skipGet()) {
            //Look in cache for existing data
            result = cache.get(cacheKey);
            if (result != null) {
                //Cache hit, return result
                return result;
            }
        }
        
        //Call the annotated method
        result = invocationContext.proceed();
        
        //Cache non-null result
        if (result != null) {
            cache.put(cacheKey, result);
        }
        
        return result;
    }

}
