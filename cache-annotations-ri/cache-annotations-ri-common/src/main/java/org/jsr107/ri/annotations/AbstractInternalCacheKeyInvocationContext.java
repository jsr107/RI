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

import javax.cache.annotation.CacheInvocationParameter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;


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
public abstract class AbstractInternalCacheKeyInvocationContext<I, A extends Annotation>
    extends AbstractInternalCacheInvocationContext<I, A>
    implements InternalCacheKeyInvocationContext<A> {

  private final StaticCacheKeyInvocationContext<A> staticCacheKeyInvocationContext;
  private final CacheInvocationParameter[] keyParameters;
  private final CacheInvocationParameter valueParameter;

  /**
   * Create a AbstractInternalCacheInvocationContext
   *
   * @param staticCacheKeyInvocationContext
   *                   The pre-processed method details data
   * @param invocation The current invocation context
   */
  public AbstractInternalCacheKeyInvocationContext(StaticCacheKeyInvocationContext<A> staticCacheKeyInvocationContext,
                                                   I invocation) {

    super(staticCacheKeyInvocationContext, invocation);

    this.staticCacheKeyInvocationContext = staticCacheKeyInvocationContext;

    final CacheInvocationParameter[] allParameters = this.getAllParameters();

    //Build array of key CacheParameterDetails from CacheParameterDetails List
    final List<CacheParameterDetails> keyParameterDetails = staticCacheKeyInvocationContext.getKeyParameters();
    this.keyParameters = new CacheInvocationParameter[keyParameterDetails.size()];
    int pIdx = 0;
    for (final CacheParameterDetails parameterDetails : keyParameterDetails) {
      final int parameterPosition = parameterDetails.getParameterPosition();
      this.keyParameters[pIdx++] = allParameters[parameterPosition];
    }

    //If this is for a CachePut get the CacheInvocationParameter for the CacheValue
    if (staticCacheKeyInvocationContext.getInterceptorType() == InterceptorType.CACHE_PUT) {
      final CachePutMethodDetails cachePutMethodDetails = (CachePutMethodDetails) staticCacheKeyInvocationContext;
      final CacheParameterDetails cacheValueParameter = cachePutMethodDetails.getCacheValueParameter();
      final int parameterPosition = cacheValueParameter.getParameterPosition();
      this.valueParameter = allParameters[parameterPosition];
    } else {
      this.valueParameter = null;
    }
  }


  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.InternalCacheKeyInvocationContext#getStaticCacheKeyInvocationContext()
   */
  @Override
  public StaticCacheKeyInvocationContext<A> getStaticCacheKeyInvocationContext() {
    return this.staticCacheKeyInvocationContext;
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractInternalCacheInvocationContext#getParameters(java.lang.Object)
   */
  @Override
  protected Object[] getParameters(I invocation) {
    // TODO Auto-generated method stub
    return null;
  }


  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractInternalCacheInvocationContext#getMethod(java.lang.Object)
   */
  @Override
  protected Method getMethod(I invocation) {
    // TODO Auto-generated method stub
    return null;
  }


  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractInternalCacheInvocationContext#getTarget(java.lang.Object)
   */
  @Override
  protected Object getTarget(I invocation) {
    // TODO Auto-generated method stub
    return null;
  }


  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheKeyInvocationContext#getKeyParameters()
   */
  @Override
  public CacheInvocationParameter[] getKeyParameters() {
    return this.keyParameters.clone();
  }

  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheKeyInvocationContext#getValueParameter()
   */
  @Override
  public CacheInvocationParameter getValueParameter() {
    return this.valueParameter;
  }
}
