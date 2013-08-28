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

import javax.cache.annotation.CacheMethodDetails;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;


/**
 * Static details about the annotated method, used with {@link javax.cache.annotation.CacheResolverFactory} to
 * find the {@link javax.cache.annotation.CacheResolver};
 *
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.annotation.CacheResult},
 *            {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemove}, or
 *            {@link javax.cache.annotation.CacheRemoveAll}.
 * @author Eric Dalquist
 * @since 1.0
 */
public class CacheMethodDetailsImpl<A extends Annotation> implements CacheMethodDetails<A> {
  private final Method targetMethod;
  private final Set<Annotation> methodAnotations;
  private final A cacheAnnotation;
  private final String cacheName;

  /**
   * Create a new set of method details
   *
   * @param targetMethod     The annotated method
   * @param methodAnotations All annotations that exist on the method
   * @param cacheAnnotation  The caching related annotation
   * @param cacheName        The resolved name of the cache for the method
   */
  public CacheMethodDetailsImpl(Method targetMethod,
                                Set<Annotation> methodAnotations, A cacheAnnotation, String cacheName) {
    if (targetMethod == null) {
      throw new IllegalArgumentException("targetMethod cannot be null");
    }
    if (methodAnotations == null) {
      throw new IllegalArgumentException("methodAnotations cannot be null");
    }
    if (cacheAnnotation == null) {
      throw new IllegalArgumentException("cacheAnnotation cannot be null");
    }
    if (cacheName == null) {
      throw new IllegalArgumentException("cacheName cannot be null");
    }

    this.targetMethod = targetMethod;
    this.methodAnotations = methodAnotations;
    this.cacheAnnotation = cacheAnnotation;
    this.cacheName = cacheName;
  }


  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheMethodDetails#getMethod()
   */
  @Override
  public Method getMethod() {
    return this.targetMethod;
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheMethodDetails#getAnnotations()
   */
  @Override
  public Set<Annotation> getAnnotations() {
    return this.methodAnotations;
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheMethodDetails#getCacheAnnotation()
   */
  @Override
  public A getCacheAnnotation() {
    return this.cacheAnnotation;
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheMethodDetails#getCacheName()
   */
  @Override
  public String getCacheName() {
    return this.cacheName;
  }
}
