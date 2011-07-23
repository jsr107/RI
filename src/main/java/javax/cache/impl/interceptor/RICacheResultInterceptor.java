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
import javax.cache.interceptor.CacheKey;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheResolver;
import javax.cache.interceptor.CacheResult;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;


/**
 * 
 * @author Rick Hightower
 * 
 */
@CacheResult @Interceptor
public class RICacheResultInterceptor {
    
    
    @Inject
    private BeanManagerUtil beanManagerUtil;


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
        CacheResult cacheResult = joinPoint.getMethod().getAnnotation(
                CacheResult.class);

        CacheResolver resolver  = getCacheResolver(cacheResult);

        Cache<Object, Object> cache = resolver.resolveCache(cacheResult.cacheName(), joinPoint.getMethod());
        
        
        CacheKeyGenerator keyGenerator = getKeyGenerator(cacheResult);
        CacheKey key = keyGenerator.generateCacheKey(joinPoint);
        
        
        Object ret = null;

        if (!cacheResult.skipGet()) {
           ret = cache.get(key);
        }

        if (ret == null) {
             ret = joinPoint.proceed();
             if (ret != null) {
                 cache.put(key, ret);
             }
        }

        return ret;

    }

    /**
     * 
     * @param cacheResult
     * @return
     */
    private CacheKeyGenerator getKeyGenerator(CacheResult cacheResult) {
        //TODO wrap the qualifiers from cacheResult.cacheKeyGeneratorQualifiers() and pass to getBeanByType
        return beanManagerUtil.getBeanByType(cacheResult.cacheKeyGenerator());
    }

    /**
     * 
     * @param cacheResult
     * @return
     */
    private CacheResolver getCacheResolver(CacheResult cacheResult) {
        //TODO wrap the qualifiers from cacheResult.cacheResolverQualifiers() and pass to getBeanByType
        return beanManagerUtil.getBeanByType(cacheResult.cacheResovler());
    }

}
