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
package org.jsr107.ri.annotations.spring.config;

import org.aopalliance.intercept.MethodInvocation;
import org.jsr107.ri.annotations.CacheContextSource;
import org.jsr107.ri.annotations.InterceptorType;
import org.jsr107.ri.annotations.StaticCacheInvocationContext;
import org.jsr107.ri.annotations.spring.CacheMethodInterceptor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * Pointcut that delegates matching checks to a {@link CacheContextSource}.
 *
 * @author Eric Dalquist
 * @version $Revision: 656 $
 */
public class CacheStaticMethodMatcherPointcut extends StaticMethodMatcherPointcut {
  private final CacheContextSource<MethodInvocation> cacheContextSource;
  private final InterceptorType interceptorType;

  /**
   * Create pointcut that uses the specific cache context source and operations on the specified interceptor type
   *
   * @param cacheContextSource Source to retrieve invocation context information from
   * @param cacheInterceptor   The type of interceptor this pointcut is for
   */
  public CacheStaticMethodMatcherPointcut(CacheContextSource<MethodInvocation> cacheContextSource, CacheMethodInterceptor cacheInterceptor) {
    Assert.notNull(cacheContextSource);
    Assert.notNull(cacheInterceptor);

    this.cacheContextSource = cacheContextSource;
    this.interceptorType = cacheInterceptor.getInterceptorType();
  }

  /**
   * Returns true if the configured {@link CacheContextSource#getMethodDetails(Method, Class)}
   * method returns an {@link StaticCacheInvocationContext} with an {@link InterceptorType} that matches
   * the type passed into the constructor
   *
   * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class)
   */
  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    final StaticCacheInvocationContext<? extends Annotation> methodDetails = this.cacheContextSource.getMethodDetails(method, targetClass);
    return methodDetails != null && methodDetails.getInterceptorType() == interceptorType;
  }
}
