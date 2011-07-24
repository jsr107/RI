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
package javax.cache.impl.interceptor;


import javax.cache.Cache;
import javax.cache.interceptor.CacheConfig;
import javax.cache.interceptor.CacheRemoveAll;
import javax.cache.interceptor.CacheResolver;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;


/**
 * 
 * @author Rick Hightower
 * 
 */
@CacheRemoveAll @Interceptor
public class RICacheRemoveAllInterceptor {

    /**
     * 
     */
    @Inject
    private RICacheLookupUtil lookup;



    /**
     * Cache Result around method.
     * 
     * @param joinPoint
     *            joinPoint
     * @return
     * @throws Exception
     *             bad thing happened
     */
    @AroundInvoke
    public Object cacheResult(InvocationContext joinPoint) throws Exception {

        /* Lookup configuration annotations. */
        CacheConfig config = joinPoint.getTarget().getClass().getAnnotation(CacheConfig.class);
        CacheRemoveAll annotation = joinPoint.getMethod().getAnnotation(
                CacheRemoveAll.class);

        /* Lookup cache. */
        CacheResolver resolver  = lookup.getCacheResolver(annotation.cacheResovler(), config);
        String cacheName = lookup.findCacheName(config, annotation.cacheName());
        Cache<Object, Object> cache = resolver.resolveCache(cacheName, joinPoint.getMethod());
        
        if (!annotation.afterInvocation()) {
            cache.removeAll();
         }

        Object ret = joinPoint.proceed();

        if (annotation.afterInvocation()) {
            cache.removeAll();
         }
        
        return ret;
        
        
    }

}
