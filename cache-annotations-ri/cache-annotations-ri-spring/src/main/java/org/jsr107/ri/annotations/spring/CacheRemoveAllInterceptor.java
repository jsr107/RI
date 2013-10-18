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

package org.jsr107.ri.annotations.spring;

import org.aopalliance.intercept.MethodInvocation;
import org.jsr107.ri.annotations.AbstractCacheRemoveAllInterceptor;
import org.jsr107.ri.annotations.CacheContextSource;
import org.jsr107.ri.annotations.InterceptorType;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class CacheRemoveAllInterceptor extends AbstractCacheRemoveAllInterceptor<MethodInvocation> implements CacheMethodInterceptor {
  private final CacheContextSource<MethodInvocation> cacheContextSource;

  /**
   * @param cacheContextSource
   */
  public CacheRemoveAllInterceptor(CacheContextSource<MethodInvocation> cacheContextSource) {
    this.cacheContextSource = cacheContextSource;
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.spring.CacheMethodInterceptor#getInterceptorType()
   */
  @Override
  public InterceptorType getInterceptorType() {
    return InterceptorType.CACHE_REMOVE_ALL;
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.spring.AbstractCacheInterceptor#invoke(org.aopalliance.intercept.MethodInvocation, java.lang.Class, javax.cache.annotation.impl.spring.CacheOperation)
   */
  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    return this.cacheRemoveAll(cacheContextSource, invocation);
  }


  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractCacheInterceptor#proceed(java.lang.Object)
   */
  @Override
  protected Object proceed(MethodInvocation invocation) throws Throwable {
    return invocation.proceed();
  }
}
