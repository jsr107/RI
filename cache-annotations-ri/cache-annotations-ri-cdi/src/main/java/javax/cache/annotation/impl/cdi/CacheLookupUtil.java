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
package javax.cache.annotation.impl.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.impl.AbstractCacheInvocationContextImpl;
import javax.cache.annotation.impl.AbstractCacheKeyInvocationContextImpl;
import javax.cache.annotation.impl.AbstractCacheLookupUtil;
import javax.cache.annotation.impl.DefaultCacheKeyGenerator;
import javax.cache.annotation.impl.DefaultCacheResolverFactory;
import javax.cache.annotation.impl.StaticCacheInvocationContext;
import javax.cache.annotation.impl.StaticCacheKeyInvocationContext;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;

/**
 * Utility used by all annotations to lookup the {@link CacheResolver} and {@link CacheKeyGenerator} for a given method.
 * 
 * @author Rick Hightower
 * 
 */
public class CacheLookupUtil extends AbstractCacheLookupUtil<InvocationContext> {
    @Inject
    private BeanManagerUtil beanManagerUtil;
    
    private CacheKeyGenerator defaultCacheKeyGenerator = new DefaultCacheKeyGenerator();
    private CacheResolverFactory defaultCacheResolverFactory = new DefaultCacheResolverFactory();

    
    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractCacheLookupUtil#createCacheKeyInvocationContextImpl(javax.cache.annotation.impl.StaticCacheKeyInvocationContext, java.lang.Object)
     */
    @Override
    protected AbstractCacheKeyInvocationContextImpl<InvocationContext> createCacheKeyInvocationContextImpl(
            StaticCacheKeyInvocationContext<? extends Annotation> staticCacheKeyInvocationContext,
            InvocationContext invocation) {
        return new CdiCacheKeyInvocationContextImpl(staticCacheKeyInvocationContext, invocation);
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractCacheLookupUtil#createCacheInvocationContextImpl(javax.cache.annotation.impl.StaticCacheInvocationContext, java.lang.Object)
     */
    @Override
    protected AbstractCacheInvocationContextImpl<InvocationContext> createCacheInvocationContextImpl(
            StaticCacheInvocationContext<? extends Annotation> staticCacheInvocationContext,
            InvocationContext invocation) {
        return new CdiCacheInvocationContextImpl(staticCacheInvocationContext, invocation);
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractCacheLookupUtil#getTarget(java.lang.Object)
     */
    @Override
    protected Object getTarget(InvocationContext invocation) {
        return invocation.getTarget();
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractCacheLookupUtil#getMethod(java.lang.Object)
     */
    @Override
    protected Method getMethod(InvocationContext invocation) {
        return invocation.getMethod();
    }
    
    

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractCacheLookupUtil#getObjectByType(java.lang.Class)
     */
    @Override
    protected <T> T getObjectByType(Class<T> type) {
        return beanManagerUtil.getBeanByType(type);
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractCacheLookupUtil#getDefaultCacheKeyGenerator()
     */
    @Override
    protected CacheKeyGenerator getDefaultCacheKeyGenerator() {
        return this.defaultCacheKeyGenerator;
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractCacheLookupUtil#getDefaultCacheResolverFactory()
     */
    @Override
    protected CacheResolverFactory getDefaultCacheResolverFactory() {
        return this.defaultCacheResolverFactory;
    }
}
