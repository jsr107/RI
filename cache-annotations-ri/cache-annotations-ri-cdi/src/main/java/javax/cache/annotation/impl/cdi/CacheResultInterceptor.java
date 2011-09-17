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


import javax.cache.annotation.CacheResult;
import javax.cache.annotation.impl.AbstractCacheResultInterceptor;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;


/**
 * Interceptor for {@link CacheResult}
 *
 * @author Rick Hightower
 * @author Eric Dalquist
 */
@CacheResult @Interceptor
public class CacheResultInterceptor extends AbstractCacheResultInterceptor<InvocationContext> {
    
    @Inject
    private CacheLookupUtil lookup;
 
    /**
     * @param invocationContext The intercepted invocation
     * @return The result from {@link InvocationContext#proceed()}
     * @throws Throwable likely {@link InvocationContext#proceed()} threw an exception
     */
    @AroundInvoke
    public Object cacheResult(InvocationContext invocationContext) throws Throwable {
        return this.cacheResult(this.lookup, invocationContext);
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractCacheInterceptor#proceed(java.lang.Object)
     */
    @Override
    protected Object proceed(InvocationContext invocation) throws Exception {
        return invocation.proceed();
    }
}
