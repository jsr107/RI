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
package javax.cache.implementation.interceptor;


import javax.cache.Cache;
import javax.cache.interceptor.CacheRemoveAll;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;


/**
 * 
 * @author Rick Hightower
 * @author Eric Dalquist
 */
@CacheRemoveAll @Interceptor
public class RICacheRemoveAllInterceptor {
    @Inject
    private RICacheLookupUtil lookup;



    /**
     * @param invocationContext The intercepted invocation
     * @return The result from {@link InvocationContext#proceed()}
     * @throws Exception likely {@link InvocationContext#proceed()} threw an exception
     */
    @AroundInvoke
    public Object cacheResult(InvocationContext invocationContext) throws Exception {
        final MethodDetails methodDetails = this.lookup.getMethodDetails(invocationContext);
        
        if (methodDetails.getInterceptorType() != InterceptorType.CACHE_REMOVE_ALL) {
            throw new IllegalStateException("AroundInvoke method for " + InterceptorType.CACHE_REMOVE_ALL + 
                    " called but MethodDetails.InterceptorType is " + methodDetails.getInterceptorType());
        }
        
        final CacheRemoveAllMethodDetails cacheRemoveAllMethodDetails = (CacheRemoveAllMethodDetails)methodDetails;
        
        final Cache<Object, Object> cache = cacheRemoveAllMethodDetails.getCache();
        
        final CacheRemoveAll cacheRemoveAllAnnotation = cacheRemoveAllMethodDetails.getCacheRemoveAllAnnotation();
        final boolean afterInvocation = cacheRemoveAllAnnotation.afterInvocation();
        
        //If pre-invocation - remove all entries
        if (!afterInvocation) {
            cache.removeAll();
        }
        
        //Call the annotated method
        final Object result = invocationContext.proceed();
        
        //If post-invocation - remove all entries
        if (afterInvocation) {
            cache.removeAll();
        }
        
        return result;
    }

}
