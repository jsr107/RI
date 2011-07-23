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
import javax.cache.CacheManager;
import javax.cache.CacheManagerFactory;
import javax.cache.interceptor.CacheKey;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheResult;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * 
 * @author Rick Hightower
 * 
 */
@Interceptor
@CacheResult
public class CacheResultInterceptor {

    /**
     * Cache Result around method.
     * 
     * @param joinPoint
     *            joinPoint
     * @return
     * @throws Exception
     *             bad thing happended
     */
    @AroundInvoke
    public Object cacheResult(InvocationContext joinPoint) throws Exception {
        CacheResult cacheResult = joinPoint.getMethod().getAnnotation(
                CacheResult.class);

        Cache<Object, Object> cache = getCache(cacheResult);

        CacheKeyGenerator keyGenerator = cacheResult.cacheKeyGenerator()
                .newInstance();
        CacheKey key = keyGenerator.generateCacheKey(joinPoint);

        if (!cache.containsKey(key)) {
            cache.put(key, joinPoint.proceed());
        }

        return cache.get(key);
    }

    private Cache<Object, Object> getCache(CacheResult cacheResult) {
        // TODO resolve the cache using cache resolver stuff from annotation
        // Use the cacheResult.cacheResolver to look the cache up in CDI.
        CacheManager defaultCacheManager = CacheManagerFactory.INSTANCE
                .getCacheManager();
        return defaultCacheManager.createCacheBuilder(cacheResult.cacheName())
                .build();
    }

}
