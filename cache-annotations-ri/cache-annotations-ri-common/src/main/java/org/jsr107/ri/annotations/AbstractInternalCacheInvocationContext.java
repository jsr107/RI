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

import javax.cache.annotation.CacheInvocationParameter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;


/**
 * Provides caching specific context about an intercepted method invocation.
 * <p/>
 * NOTE: This class uses arrays instead of immutable collections due to the frequency of these objects
 * being created. In testing creating new arrays and cloning them requires less CPU time and fewer objects
 * than creating new immutable Lists.
 *
 * @param <I> The intercepted method invocation
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.annotation.CacheResult},
 *            {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemove}, or
 *            {@link javax.cache.annotation.CacheRemoveAll}.
 * @author Eric Dalquist
 * @since 1.0
 */
public abstract class AbstractInternalCacheInvocationContext<I, A extends Annotation> implements InternalCacheInvocationContext<A> {
  private final StaticCacheInvocationContext<A> abstractStaticCacheInvocationContext;
  private final I invocation;
  private final CacheInvocationParameter[] allParameters;

  /**
   * Create a AbstractInternalCacheInvocationContext
   *
   * @param staticCacheInvocationContext The pre-processed method details data
   * @param invocation                   The current invocation context
   */
  public AbstractInternalCacheInvocationContext(StaticCacheInvocationContext<A> staticCacheInvocationContext,
                                                I invocation) {

    this.abstractStaticCacheInvocationContext = staticCacheInvocationContext;
    this.invocation = invocation;

    final Object[] parameters = getParameters(invocation);

    //Build array of all CacheInvocationParameter from CacheParameterDetails List
    final List<CacheParameterDetails> allParameterDetails = staticCacheInvocationContext.getAllParameters();
    this.allParameters = new CacheInvocationParameter[allParameterDetails.size()];
    for (final CacheParameterDetails parameterDetails : allParameterDetails) {
      final int parameterPosition = parameterDetails.getParameterPosition();
      this.allParameters[parameterPosition] = new CacheInvocationParameterImpl(parameterDetails, parameters[parameterPosition]);
    }
  }

  /**
   * @param invocation The intercepted method invocation
   * @return The parameters to the method invocation
   */
  protected abstract Object[] getParameters(I invocation);

  /**
   * @param invocation The intercepted method invocation
   * @return The method that was intercepted
   */
  protected abstract Method getMethod(I invocation);

  /**
   * @param invocation The intercepted method invocation
   * @return The object targeted by the invocation
   */
  protected abstract Object getTarget(I invocation);

  /**
   * @return the abstractStaticCacheInvocationContext
   */
  @Override
  public StaticCacheInvocationContext<A> getStaticCacheInvocationContext() {
    return this.abstractStaticCacheInvocationContext;
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheMethodDetails#getAnnotations()
   */
  @Override
  public Set<Annotation> getAnnotations() {
    return this.abstractStaticCacheInvocationContext.getAnnotations();
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheMethodDetails#getCacheAnnotation()
   */
  @Override
  public A getCacheAnnotation() {
    return this.abstractStaticCacheInvocationContext.getCacheAnnotation();
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheMethodDetails#getCacheName()
   */
  @Override
  public String getCacheName() {
    return this.abstractStaticCacheInvocationContext.getCacheName();
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheMethodDetails#getMethod()
   */
  @Override
  public Method getMethod() {
    return this.getMethod(this.invocation);
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheInvocationContext#getTarget()
   */
  @Override
  public Object getTarget() {
    return this.getTarget(this.invocation);
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheInvocationContext#getAllParameters()
   */
  @Override
  public CacheInvocationParameter[] getAllParameters() {
    return this.allParameters.clone();
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheInvocationContext#unwrap(java.lang.Class)
   */
  @Override
  public <T> T unwrap(Class<T> cls) {
    if (cls.isAssignableFrom(this.invocation.getClass())) {
      return cls.cast(this.invocation);
    }

    throw new IllegalArgumentException("Unwapping to " + cls + " is not a supported by this implementation");
  }
}
