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
package org.jsr107.ri.annotations;


import javax.cache.Cache;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolver;
import java.lang.annotation.Annotation;


/**
 * Interceptor for {@link CacheRemoveAll}
 * 
 * @author Rick Hightower
 * @author Eric Dalquist
 * @param <I> The intercepted method invocation
 * @since 1.0
 */
public abstract class AbstractCacheRemoveAllInterceptor<I> extends AbstractCacheInterceptor<I> {
    /**
     * Handles the {@link Cache#removeAll()} as specified for the {@link CacheRemoveAll} annotation
     * 
     * @param cacheContextSource The intercepted invocation
     * @param invocation The intercepted invocation
     * @return The result from {@link #proceed(Object)}
     * @throws Throwable if {@link #proceed(Object)} threw
     */
    public final Object cacheRemoveAll(CacheContextSource<I> cacheContextSource, I invocation) throws Throwable {
        final InternalCacheInvocationContext<? extends Annotation> cacheInvocationContext = cacheContextSource.getCacheInvocationContext(invocation);
        
        final StaticCacheInvocationContext<CacheRemoveAll> methodDetails = 
                this.getCacheInvocationContext(cacheInvocationContext, InterceptorType.CACHE_REMOVE_ALL);
        
        final CacheRemoveAll cacheRemoveAllAnnotation = methodDetails.getCacheAnnotation();
        final boolean afterInvocation = cacheRemoveAllAnnotation.afterInvocation();
        
        //If pre-invocation - remove all entries
        if (!afterInvocation) {
            removeAll(cacheInvocationContext, methodDetails);
        }
        
        final Object result;
        try {
            //Call the annotated method
            result = this.proceed(invocation);
        } catch (Throwable t) {
            if (afterInvocation) {
                //If after invocation is true and if the throwable passes the include/exclude filters and then call removeAll
                final Class<? extends Throwable>[] evictFor = cacheRemoveAllAnnotation.evictFor();
                final Class<? extends Throwable>[] noEvictFor = cacheRemoveAllAnnotation.noEvictFor();
                
                //Check for empty/null here since isIncluded returns true for those cases
                final boolean cache = ClassFilter.isIncluded(t, evictFor, noEvictFor, false);
                
                //Exception is included
                if (cache) {
                    removeAll(cacheInvocationContext, methodDetails);
                }
            }

            throw t;
        }
        
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
    protected void removeAll(final InternalCacheInvocationContext<? extends Annotation> cacheInvocationContext,
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
            final InternalCacheInvocationContext<? extends Annotation> cacheInvocationContext, final InterceptorType interceptorType) {
        
        final StaticCacheInvocationContext<? extends Annotation> staticCacheInvocationContext = 
                cacheInvocationContext.getStaticCacheInvocationContext();
        
        if (staticCacheInvocationContext.getInterceptorType() != interceptorType) {
            throw new IllegalStateException("AroundInvoke method for " + interceptorType + " called but MethodDetails.InterceptorType is " + 
                    staticCacheInvocationContext.getInterceptorType());
        }
        
        return (T)staticCacheInvocationContext;
    }

}
