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

package org.jsr107.ri.annotations.cdi;

import org.jsr107.ri.annotations.AbstractInternalCacheInvocationContext;
import org.jsr107.ri.annotations.StaticCacheInvocationContext;

import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * CDI specific cache invocation context using {@link InvocationContext}
 *
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.annotation.CacheResult},
 *            {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemove}, or
 *            {@link javax.cache.annotation.CacheRemoveAll}.
 * @author Eric Dalquist
 * @since 1.0
 */
public class CdiCacheInvocationContextImpl<A extends Annotation> extends AbstractInternalCacheInvocationContext<InvocationContext, A> {

  /**
   * Create new cache invocation context for the static context and invocation
   *
   * @param staticCacheInvocationContext Static information about the invoked method
   * @param invocation                   The CDI invocation context
   */
  public CdiCacheInvocationContextImpl(
      StaticCacheInvocationContext<A> staticCacheInvocationContext,
      InvocationContext invocation) {

    super(staticCacheInvocationContext, invocation);
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractInternalCacheInvocationContext#getParameters(java.lang.Object)
   */
  @Override
  protected Object[] getParameters(InvocationContext invocation) {
    return invocation.getParameters();
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractInternalCacheInvocationContext#getMethod(java.lang.Object)
   */
  @Override
  protected Method getMethod(InvocationContext invocation) {
    return invocation.getMethod();
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractInternalCacheInvocationContext#getTarget(java.lang.Object)
   */
  @Override
  protected Object getTarget(InvocationContext invocation) {
    return invocation.getTarget();
  }
}
