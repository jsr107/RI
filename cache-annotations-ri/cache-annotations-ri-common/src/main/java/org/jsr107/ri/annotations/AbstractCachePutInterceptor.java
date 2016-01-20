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
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.GeneratedCacheKey;

import java.lang.annotation.Annotation;


/**
 * Interceptor for {@link CachePut}
 *
 * @param <I> The intercepted method invocation
 * @param <E> The exception type that is thrown
 * @author Rick Hightower
 * @author Eric Dalquist
 * @since 1.0
 */
public abstract class AbstractCachePutInterceptor<I, E extends Throwable> extends AbstractKeyedCacheInterceptor<I, E, CachePutMethodDetails> {

  /**
   * Handles the {@link Cache#put(Object, Object)} as specified for the {@link CachePut} annotation
   *
   * @param cacheContextSource The intercepted invocation
   * @param invocation         The intercepted invocation
   * @return The result from {@link #proceed(Object)}
   * @throws E if {@link #proceed(Object)} threw
   */
  public Object cachePut(CacheContextSource<I> cacheContextSource, I invocation) throws E {
    final InternalCacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext =
        cacheContextSource.getCacheKeyInvocationContext(invocation);
    final CachePutMethodDetails methodDetails = this.getStaticCacheKeyInvocationContext(cacheKeyInvocationContext, InterceptorType.CACHE_PUT);

    final CachePut cachePutAnnotation = methodDetails.getCacheAnnotation();
    final boolean afterInvocation = cachePutAnnotation.afterInvocation();

    final CacheInvocationParameter valueParameter = cacheKeyInvocationContext.getValueParameter();
    final Object value = valueParameter.getValue();

    if (!afterInvocation) {
      cacheValue(cacheKeyInvocationContext, methodDetails, value);
    }

    final Object result;
    try {
      //Call the annotated method
      result = this.proceed(invocation);
    } catch (Throwable t) {
      if (afterInvocation) {
        //If after invocation is true and if the throwable passes the include/exclude filters and then call put
        final Class<? extends Throwable>[] cacheFor = cachePutAnnotation.cacheFor();
        final Class<? extends Throwable>[] noCacheFor = cachePutAnnotation.noCacheFor();

        //Check for empty/null here since isIncluded returns true for those cases
        final boolean cache = ClassFilter.isIncluded(t, cacheFor, noCacheFor, false);

        //Exception is included
        if (cache) {
          cacheValue(cacheKeyInvocationContext, methodDetails, value);
        }
      }

      throw t;
    }

    if (afterInvocation) {
      cacheValue(cacheKeyInvocationContext, methodDetails, value);
    }

    return result;
  }


  /**
   * Lookup the Cache, generate a GeneratedCacheKey and store the value in the cache.
   *
   * @param cacheKeyInvocationContext The invocation context
   * @param methodDetails             The details about the cached method
   * @param value                     The value to cache
   */
  protected void cacheValue(final InternalCacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext,
                            final CachePutMethodDetails methodDetails, final Object value) {

    final Object cachedValue = value;

    final CacheResolver cacheResolver = methodDetails.getCacheResolver();
    final Cache<Object, Object> cache = cacheResolver.resolveCache(cacheKeyInvocationContext);

    final CacheKeyGenerator cacheKeyGenerator = methodDetails.getCacheKeyGenerator();
    final GeneratedCacheKey cacheKey = cacheKeyGenerator.generateCacheKey(cacheKeyInvocationContext);

    cache.put(cacheKey, cachedValue);
  }
}
