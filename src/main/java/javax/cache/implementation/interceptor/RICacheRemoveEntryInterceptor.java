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


import java.lang.reflect.Method;

import javax.cache.Cache;
import javax.cache.interceptor.CachingDefaults;
import javax.cache.interceptor.CacheKey;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheRemoveEntry;
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
@CacheRemoveEntry @Interceptor
public class RICacheRemoveEntryInterceptor {
    
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
        
        /* Get annotation configuration. */
        CachingDefaults config = joinPoint.getTarget().getClass().getAnnotation(CachingDefaults.class);
        Method method = joinPoint.getMethod();
        CacheRemoveEntry annotation = joinPoint.getMethod().getAnnotation(CacheRemoveEntry.class);

        /* Lookup cache. */
        CacheResolver resolver  = lookup.getCacheResolver(annotation.cacheResolver(), config, method);
        String cacheName = lookup.findCacheName(config, annotation.cacheName(), method, false);
        Cache<Object, Object> cache = resolver.resolveCache(cacheName, joinPoint.getMethod());
        
        /* Generate key. */
        CacheKeyGenerator keyGenerator = lookup.getKeyGenerator(annotation.cacheKeyGenerator(), config, method);
        CacheKey key = keyGenerator.generateCacheKey(joinPoint);

        
        if (!annotation.afterInvocation()) {
            cache.remove(key);
         }

        Object ret = joinPoint.proceed();

        if (annotation.afterInvocation()) {
            cache.remove(key);
         }
        
        return ret;
    }

}
