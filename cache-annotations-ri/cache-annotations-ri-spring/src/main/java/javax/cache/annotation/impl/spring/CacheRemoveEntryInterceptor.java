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

package javax.cache.annotation.impl.spring;

import javax.cache.annotation.impl.AbstractCacheRemoveEntryInterceptor;
import javax.cache.annotation.impl.CacheContextSource;
import javax.cache.annotation.impl.InterceptorType;

import org.aopalliance.intercept.MethodInvocation;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class CacheRemoveEntryInterceptor extends AbstractCacheRemoveEntryInterceptor<MethodInvocation> implements CacheMethodInterceptor {
    private final CacheContextSource<MethodInvocation> cacheContextSource;
    
    /**
     * @param cacheContextSource
     */
    public CacheRemoveEntryInterceptor(CacheContextSource<MethodInvocation> cacheContextSource) {
        this.cacheContextSource = cacheContextSource;
    }
    
    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.spring.CacheMethodInterceptor#getInterceptorType()
     */
    @Override
    public InterceptorType getInterceptorType() {
        return InterceptorType.CACHE_REMOVE_ENTRY;
    }


    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.spring.AbstractCacheInterceptor#invoke(org.aopalliance.intercept.MethodInvocation, java.lang.Class, javax.cache.annotation.impl.spring.CacheOperation)
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return this.cacheRemoveEntry(cacheContextSource, invocation);
    }
    
    
    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractCacheInterceptor#proceed(java.lang.Object)
     */
    @Override
    protected Object proceed(MethodInvocation invocation) throws Throwable {
        return invocation.proceed();
    }
}
