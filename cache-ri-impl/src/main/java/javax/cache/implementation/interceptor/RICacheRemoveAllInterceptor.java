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


import java.lang.annotation.Annotation;

import javax.cache.Cache;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolver;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;


/**
 * Interceptor for {@link CacheRemoveAll}
 * 
 * @author Rick Hightower
 * @author Eric Dalquist
 */
@CacheRemoveAll @Interceptor
public class RICacheRemoveAllInterceptor {
    @Inject
    private RICacheLookupUtil lookup;

    /**
     * @param invocationContext The intercepted invocation
     * @return The result from {@link InvocationContext#proceed()}
     * @throws Exception likely {@link InvocationContext#proceed()} threw an exception
     */
    @AroundInvoke
    public Object cacheResult(InvocationContext invocationContext) throws Exception {
        final CacheInvocationContextImpl cacheInvocationContext = this.lookup.getCacheInvocationContext(invocationContext);
        
        final StaticCacheInvocationContext<CacheRemoveAll> methodDetails = 
                this.getCacheInvocationContext(cacheInvocationContext, InterceptorType.CACHE_REMOVE_ALL);
        
        final CacheRemoveAll cacheRemoveAllAnnotation = methodDetails.getCacheAnnotation();
        final boolean afterInvocation = cacheRemoveAllAnnotation.afterInvocation();
        
        //If pre-invocation - remove all entries
        if (!afterInvocation) {
            removeAll(cacheInvocationContext, methodDetails);
        }
        
        //Call the annotated method
        final Object result = invocationContext.proceed();
        
        //If post-invocation - remove all entries
        if (afterInvocation) {
            removeAll(cacheInvocationContext, methodDetails);
        }
        
        return result;
    }

    /**
     * Resolve the Cache and call removeAll
     * 
     * @param cacheInvocationContext The invocation context 
     * @param methodDetails The details about the cached method
     */
    protected void removeAll(final CacheInvocationContextImpl cacheInvocationContext,
            final StaticCacheInvocationContext<CacheRemoveAll> methodDetails) {
        final CacheResolver cacheResolver = methodDetails.getCacheResolver();
        final Cache<Object, Object> cache = cacheResolver.resolveCache(cacheInvocationContext);
        cache.removeAll();
    }
    
    /**
     * Get, check the {@link InterceptorType} and cast the {@link CacheMethodDetailsImpl} for the invocation.
     * 
     * @param cacheInvocationContext The invocation context to get the {@link CacheMethodDetailsImpl} from.
     * @param interceptorType The current interceptor type, used for validation.
     * @return The casted {@link CacheMethodDetailsImpl} object.
     */
    @SuppressWarnings("unchecked")
    protected <T extends StaticCacheInvocationContext<?>> T getCacheInvocationContext(
            final CacheInvocationContextImpl cacheInvocationContext, final InterceptorType interceptorType) {
        
        final StaticCacheInvocationContext<? extends Annotation> staticCacheInvocationContext = 
                cacheInvocationContext.getStaticCacheInvocationContext();
        
        if (staticCacheInvocationContext.getInterceptorType() != interceptorType) {
            throw new IllegalStateException("AroundInvoke method for " + interceptorType + " called but MethodDetails.InterceptorType is " + 
                    staticCacheInvocationContext.getInterceptorType());
        }
        
        return (T)staticCacheInvocationContext;
    }

}
