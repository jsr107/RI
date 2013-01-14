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
package org.jsr107.ri.annotations.cdi;

import org.jsr107.ri.annotations.AbstractCacheLookupUtil;
import org.jsr107.ri.annotations.DefaultCacheKeyGenerator;
import org.jsr107.ri.annotations.DefaultCacheResolverFactory;
import org.jsr107.ri.annotations.InternalCacheInvocationContext;
import org.jsr107.ri.annotations.InternalCacheKeyInvocationContext;
import org.jsr107.ri.annotations.StaticCacheInvocationContext;
import org.jsr107.ri.annotations.StaticCacheKeyInvocationContext;

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheResolverFactory;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Utility used by all annotations to lookup the {@link javax.cache.annotation.CacheResolver} and {@link CacheKeyGenerator} for a given method.
 * 
 * @author Rick Hightower
 * @since 1.0
 */
public class CacheLookupUtil extends AbstractCacheLookupUtil<InvocationContext> {
    @Inject
    private BeanManagerUtil beanManagerUtil;
    
    private CacheKeyGenerator defaultCacheKeyGenerator = new DefaultCacheKeyGenerator();
    private CacheResolverFactory defaultCacheResolverFactory = new DefaultCacheResolverFactory();

    
    /*
     * Annoation type cannot be known at compile time so ignore the warning
     * 
     * (non-Javadoc)
     * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#createCacheKeyInvocationContextImpl(javax.cache.annotation.impl.StaticCacheKeyInvocationContext, java.lang.Object)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected InternalCacheKeyInvocationContext<? extends Annotation> createCacheKeyInvocationContextImpl(
            StaticCacheKeyInvocationContext<? extends Annotation> staticCacheKeyInvocationContext,
            InvocationContext invocation) {
        return new CdiCacheKeyInvocationContextImpl(staticCacheKeyInvocationContext, invocation);
    }

    /* 
     * Annoation type cannot be known at compile time so ignore the warning
     * 
     * (non-Javadoc)
     * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#createCacheInvocationContextImpl(javax.cache.annotation.impl.AbstractStaticCacheInvocationContext, java.lang.Object)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected InternalCacheInvocationContext<? extends Annotation> createCacheInvocationContextImpl(
            StaticCacheInvocationContext<? extends Annotation> staticCacheInvocationContext,
            InvocationContext invocation) {
        return new CdiCacheInvocationContextImpl(staticCacheInvocationContext, invocation);
    }

    /* (non-Javadoc)
     * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#getTarget(java.lang.Object)
     */
    @Override
    protected Class<?> getTargetClass(InvocationContext invocation) {
        return invocation.getMethod().getDeclaringClass();
    }

    /* (non-Javadoc)
     * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#getMethod(java.lang.Object)
     */
    @Override
    protected Method getMethod(InvocationContext invocation) {
        return invocation.getMethod();
    }
    
    

    /* (non-Javadoc)
     * @see org.jsr107.ri.annotations.AbstractCacheLookupUtil#getObjectByType(java.lang.Class)
     */
    @Override
    protected <T> T getObjectByType(Class<T> type) {
        return beanManagerUtil.getBeanByType(type);
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
