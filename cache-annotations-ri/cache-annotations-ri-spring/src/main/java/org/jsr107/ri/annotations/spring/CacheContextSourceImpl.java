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

package org.jsr107.ri.annotations.spring;

import org.aopalliance.intercept.MethodInvocation;
import org.jsr107.ri.annotations.AbstractCacheLookupUtil;
import org.jsr107.ri.annotations.CacheContextSource;
import org.jsr107.ri.annotations.InternalCacheInvocationContext;
import org.jsr107.ri.annotations.InternalCacheKeyInvocationContext;
import org.jsr107.ri.annotations.StaticCacheInvocationContext;
import org.jsr107.ri.annotations.StaticCacheKeyInvocationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheResolverFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class CacheContextSourceImpl
    extends AbstractCacheLookupUtil<MethodInvocation>
    implements CacheContextSource<MethodInvocation>, BeanFactoryAware {

  private CacheKeyGenerator defaultCacheKeyGenerator;
  private CacheResolverFactory defaultCacheResolverFactory;
  private BeanFactory beanFactory;

  /**
   * @param defaultCacheKeyGenerator the defaultCacheKeyGenerator to set
   */
  public void setDefaultCacheKeyGenerator(CacheKeyGenerator defaultCacheKeyGenerator) {
    this.defaultCacheKeyGenerator = defaultCacheKeyGenerator;
  }

  /**
   * @param defaultCacheResolverFactory the defaultCacheResolverFactory to set
   */
  public void setDefaultCacheResolverFactory(CacheResolverFactory defaultCacheResolverFactory) {
    this.defaultCacheResolverFactory = defaultCacheResolverFactory;
  }

  /* (non-Javadoc)
   * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  /*
   * Annoation type cannot be known at compile time so ignore the warning
   *
   * (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#createCacheKeyInvocationContextImpl(javax.cache.annotation.impl.StaticCacheKeyInvocationContext, java.lang.Object)
   */
  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  protected InternalCacheKeyInvocationContext<? extends Annotation> createCacheKeyInvocationContextImpl(
      StaticCacheKeyInvocationContext<? extends Annotation> staticCacheKeyInvocationContext,
      MethodInvocation invocation) {
    return new SpringCacheKeyInvocationContextImpl(staticCacheKeyInvocationContext, invocation);
  }

  /*
   * Annoation type cannot be known at compile time so ignore the warning
   *
   * (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#createCacheInvocationContextImpl(javax.cache.annotation.impl.AbstractStaticCacheInvocationContext, java.lang.Object)
   */
  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  protected InternalCacheInvocationContext<? extends Annotation> createCacheInvocationContextImpl(
      StaticCacheInvocationContext<? extends Annotation> staticCacheInvocationContext, MethodInvocation invocation) {
    return new SpringCacheInvocationContextImpl(staticCacheInvocationContext, invocation);
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#getTarget(java.lang.Object)
   */
  @Override
  protected Class<?> getTargetClass(MethodInvocation invocation) {
    return invocation.getThis().getClass();
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#getMethod(java.lang.Object)
   */
  @Override
  protected Method getMethod(MethodInvocation invocation) {
    return invocation.getMethod();
  }

  @Override
  protected <T extends Annotation> T getAnnotation(Class<T> annotationClass, Method method, Class<? extends Object> targetClass) {
    // The method may be on an interface, but we need attributes from the target class.
    // If the target class is null, the method will be unchanged.
    Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
    // If we are dealing with method with generic parameters, find the original method.
    specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

    final T annotation = specificMethod.getAnnotation(annotationClass);
    if (annotation != null) {
      return annotation;
    }

    if (specificMethod != method) {
      // Fallback is to look at the original method.
      return method.getAnnotation(annotationClass);
    }

    return null;
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#getObjectByType(java.lang.Class)
   */
  @Override
  protected <T> T getObjectByType(Class<T> type) {
    return this.beanFactory.getBean(type);
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#getDefaultCacheKeyGenerator()
   */
  @Override
  protected CacheKeyGenerator getDefaultCacheKeyGenerator() {
    return this.defaultCacheKeyGenerator;
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#getDefaultCacheResolverFactory()
   */
  @Override
  protected CacheResolverFactory getDefaultCacheResolverFactory() {
    return this.defaultCacheResolverFactory;
  }
}
