/**
 *  Copyright 2011-2013 Terracotta, Inc.
 *  Copyright 2011-2013 Oracle America Incorporated
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
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.GeneratedCacheKey;

import java.lang.annotation.Annotation;


/**
 * Interceptor for {@link javax.cache.annotation.CacheRemove}
 *
 * @param <I> The intercepted method invocation
 * @param <E> The exception type that is thrown
 * @author Rick Hightower
 * @author Eric Dalquist
 * @since 1.0
 */
public abstract class AbstractCacheRemoveEntryInterceptor<I, E extends Throwable> 
  extends AbstractKeyedCacheInterceptor<I, E, CacheRemoveEntryMethodDetails> {

  /**
   * Handles the {@link Cache#remove(Object)} as specified for the {@link javax.cache.annotation.CacheRemove} annotation
   *
   * @param cacheContextSource The intercepted invocation
   * @param invocation         The intercepted invocation
   * @return The result from {@link #proceed(Object)}
   * @throws E if {@link #proceed(Object)} threw
   */
  public final Object cacheRemoveEntry(CacheContextSource<I> cacheContextSource, I invocation) throws E {
    final InternalCacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext =
        cacheContextSource.getCacheKeyInvocationContext(invocation);
    final CacheRemoveEntryMethodDetails methodDetails =
        this.getStaticCacheKeyInvocationContext(cacheKeyInvocationContext, InterceptorType.CACHE_REMOVE_ENTRY);

    final CacheRemove cacheRemoveAnnotation = methodDetails.getCacheAnnotation();
    final boolean afterInvocation = cacheRemoveAnnotation.afterInvocation();

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
        final Class<? extends Throwable>[] evictFor = cacheRemoveAnnotation.evictFor();
        final Class<? extends Throwable>[] noEvictFor = cacheRemoveAnnotation.noEvictFor();

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
   * @param methodDetails             The details about the cached method
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
