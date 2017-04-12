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
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.GeneratedCacheKey;
import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Interceptor for {@link CacheResult}
 *
 * @param <I> The intercepted method invocation
 * @author Rick Hightower
 * @author Eric Dalquist
 * @since 1.0
 */
public abstract class AbstractCacheResultInterceptor<I> extends AbstractKeyedCacheInterceptor<I, CacheResultMethodDetails> {

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());


  /**
   * Handles the {@link Cache#get(Object)} and {@link Cache#put(Object, Object)} logic as specified for the
   * {@link CacheResult} annotation
   *
   * @param cacheContextSource The intercepted invocation
   * @param invocation         The intercepted invocation
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
    final Cache<Object, Throwable> exceptionCache = getExceptionCache(cacheKeyInvocationContext, methodDetails);

    //Generate the cache key
    final CacheKeyGenerator cacheKeyGenerator = methodDetails.getCacheKeyGenerator();
    final GeneratedCacheKey cacheKey = cacheKeyGenerator.generateCacheKey(cacheKeyInvocationContext);

    final CacheResult cacheResultAnnotation = methodDetails.getCacheAnnotation();

    //If skip-get is false check for a cached result or a cached exception
    Object result = null;
    if (!cacheResultAnnotation.skipGet()) {
      //Look in cache for existing data
      try {
        result = cache.get(cacheKey);
      } catch (RuntimeException ex) {
        this.logger.log(Level.SEVERE, "Error getting value from the cache, returning non-cached value.", ex);
      }
      if (result != null) {
        //Cache hit, return result
        return result;
      }

      //Look for a cached exception
      checkForCachedException(exceptionCache, cacheKey);
    }

    try {
      //Call the annotated method
      result = this.proceed(invocation);

      //Cache non-null result
      if (result != null) {
        try {
          cache.put(cacheKey, result);
        } catch (RuntimeException ex) {
          this.logger.log(Level.SEVERE, "Error putting value to the cache.", ex);
        }
      }

      return result;
    } catch (Throwable t) {
      //If exception caching is enabled check if the throwable passes the include/exclude filters and then cache it
      cacheException(exceptionCache, cacheKey, cacheResultAnnotation, t);

      throw t;
    }
  }

  /**
   * Check to see if there is a cached exception that needs to be re-thrown
   *
   * @param exceptionCache The exception cache, may be null if no exception caching is being done
   * @param cacheKey       The cache key
   * @throws Throwable The cached exception
   */
  protected void checkForCachedException(final Cache<Object, Throwable> exceptionCache, final GeneratedCacheKey cacheKey)
      throws Throwable {
    if (exceptionCache == null) {
      return;
    }

    Throwable throwable = null;
    try {
      throwable = exceptionCache.get(cacheKey);
    } catch (RuntimeException ex) {
      this.logger.log(Level.SEVERE, "Error getting exception from exception cache.", ex);
    }
    if (throwable != null) {
      //Found exception, re-throw
      throw throwable;
    }
  }

  /**
   * Cache the exception if exception caching is enabled.
   *
   * @param exceptionCache        The exception cache, may be null if no exception caching is being done
   * @param cacheKey              The cache key
   * @param cacheResultAnnotation The cache result annotation
   * @param t                     The exception to cache
   */
  protected void cacheException(final Cache<Object, Throwable> exceptionCache, final GeneratedCacheKey cacheKey,
                                final CacheResult cacheResultAnnotation, Throwable t) {
    if (exceptionCache == null) {
      return;
    }

    final Class<? extends Throwable>[] cachedExceptions = cacheResultAnnotation.cachedExceptions();
    final Class<? extends Throwable>[] nonCachedExceptions = cacheResultAnnotation.nonCachedExceptions();
    final boolean included = ClassFilter.isIncluded(t, cachedExceptions, nonCachedExceptions, true);
    if (included) {
      //Cache the exception for future rethrow
      exceptionCache.put(cacheKey, t);
    }
  }

  /**
   * Get the exception cache if one is configured
   *
   * @param cacheKeyInvocationContext The invocation details
   * @param methodDetails             The method details
   * @return The exception cache, null if exception caching is disabled.
   */
  protected Cache<Object, Throwable> getExceptionCache(
      final InternalCacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext,
      final CacheResultMethodDetails methodDetails) {

    final CacheResolver exceptionCacheResolver = methodDetails.getExceptionCacheResolver();
    if (exceptionCacheResolver != null) {
      return exceptionCacheResolver.resolveCache(cacheKeyInvocationContext);
    }

    return null;
  }
}
