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
import javax.cache.annotation.GeneratedCacheKey;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheRemoveEntry;
import javax.cache.annotation.CacheResolver;
import java.lang.annotation.Annotation;


/**
 * Interceptor for {@link CacheRemoveEntry}
 * 
 * @author Rick Hightower
 * @author Eric Dalquist
 * @param <I> The intercepted method invocation
 * @since 1.0
 */
public abstract class AbstractCacheRemoveEntryInterceptor<I> extends AbstractKeyedCacheInterceptor<I, CacheRemoveEntryMethodDetails> {

    /**
     * Handles the {@link Cache#remove(Object)} as specified for the {@link CacheRemoveEntry} annotation
     * 
     * @param cacheContextSource The intercepted invocation
     * @param invocation The intercepted invocation
     * @return The result from {@link #proceed(Object)}
     * @throws Throwable if {@link #proceed(Object)} threw
     */
    public final Object cacheRemoveEntry(CacheContextSource<I> cacheContextSource, I invocation) throws Throwable {
        final InternalCacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext = 
                cacheContextSource.getCacheKeyInvocationContext(invocation);
        final CacheRemoveEntryMethodDetails methodDetails = 
                this.getStaticCacheKeyInvocationContext(cacheKeyInvocationContext, InterceptorType.CACHE_REMOVE_ENTRY);
        
        final CacheRemoveEntry cacheRemoveEntryAnnotation = methodDetails.getCacheAnnotation();
        final boolean afterInvocation = cacheRemoveEntryAnnotation.afterInvocation();
        
        //If pre-invocation - remove entry
        if (!afterInvocation) {
            cacheRemove(cacheKeyInvocationContext, methodDetails);
        }
        
        final Object result;
        try {
            //Call the annotated method
            result = this.proceed(invocation);
        } catch (Throwable t) {
            if (afterInvocation) {
                //If after invocation is true and if the throwable passes the include/exclude filters and then call remove
                final Class<? extends Throwable>[] evictFor = cacheRemoveEntryAnnotation.evictFor();
                final Class<? extends Throwable>[] noEvictFor = cacheRemoveEntryAnnotation.noEvictFor();
                
                //Check for empty/null here since isIncluded returns true for those cases
                final boolean cache = ClassFilter.isIncluded(t, evictFor, noEvictFor, false);
                
                //Exception is included
                if (cache) {
                    cacheRemove(cacheKeyInvocationContext, methodDetails);
                }
            }

            throw t;
        }
        
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
    private void cacheRemove(final InternalCacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext,
            final CacheRemoveEntryMethodDetails methodDetails) {
        
        final CacheResolver cacheResolver = methodDetails.getCacheResolver();
        final Cache<Object, Object> cache = cacheResolver.resolveCache(cacheKeyInvocationContext);

        final CacheKeyGenerator cacheKeyGenerator = methodDetails.getCacheKeyGenerator();
        final GeneratedCacheKey cacheKey = cacheKeyGenerator.generateCacheKey(cacheKeyInvocationContext);
        
        cache.remove(cacheKey);
    }
}
