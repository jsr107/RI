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

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.interceptor.CacheKey;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheResolver;
import javax.cache.interceptor.CacheResult;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
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
 
    /**
     * 
     */
    @Inject 
    private BeanManager beanManager;
    
    /**
     * 
     * @param <T>
     * @param type
     * @param qualifiers
     * @return
     */
    private <T> T getBeanByType(Class<T> type, Annotation... qualifiers) {
        if (type == null) {
            throw new IllegalArgumentException("CDI Bean type cannot be null");
        }

        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        if (beans.isEmpty()) {
            return null;
        }
        Bean<?> bean = beanManager.resolve(beans);
        CreationalContext<?> context = beanManager
                .createCreationalContext(bean);
        @SuppressWarnings("unchecked")
        T result = (T) beanManager.getReference(bean, bean.getBeanClass(),
                context);
        return result;
    }


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

        if (!cache.containsKey(key)) {
            Object value = joinPoint.proceed();
            if (value != null) {
                cache.put(key, value);
            }
        }

        return cache.get(key);
    }

    /**
     * 
     * @param cacheResult
     * @return
     */
    private CacheKeyGenerator getKeyGenerator(CacheResult cacheResult) {
        //TODO wrap the qualifiers from cacheResult.cacheKeyGeneratorQualifiers() and pass to getBeanByType
        return getBeanByType(cacheResult.cacheKeyGenerator());
    }

    /**
     * 
     * @param cacheResult
     * @return
     */
    private CacheResolver getCacheResolver(CacheResult cacheResult) {
        //TODO wrap the qualifiers from cacheResult.cacheResolverQualifiers() and pass to getBeanByType
        return getBeanByType(cacheResult.cacheResovler());
    }

}
