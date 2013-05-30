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

package org.jsr107.ri.annotations.guice;

import org.aopalliance.intercept.MethodInvocation;
import org.jsr107.ri.annotations.AbstractCacheRemoveAllInterceptor;
import org.jsr107.ri.annotations.CacheContextSource;
import org.jsr107.ri.annotations.InterceptorType;

import javax.inject.Inject;

/**
 * @author Michael Stachel
 * @version $Revision$
 */
public class CacheRemoveAllInterceptor extends AbstractCacheRemoveAllInterceptor<MethodInvocation>
    implements CacheMethodInterceptor {

  private CacheContextSource<MethodInvocation> cacheContextSource;

  /**
   * @param cacheContextSource the CacheContextSource to use
   */
  @Inject
  public void setCacheContextSource(CacheContextSource<MethodInvocation> cacheContextSource) {
    this.cacheContextSource = cacheContextSource;
  }

  @Override
  public InterceptorType getInterceptorType() {
    return InterceptorType.CACHE_REMOVE_ALL;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    return this.cacheRemoveAll(cacheContextSource, invocation);
  }

  @Override
  protected Object proceed(MethodInvocation invocation) throws Throwable {
    return invocation.proceed();
  }

}
