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

import java.lang.reflect.Method;

import javax.cache.interceptor.CacheConfig;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheResolver;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;

/**
 * 
 * @author Rick Hightower
 *
 */
public class RICacheLookupUtil {
    
    /**
     * 
     */
    @Inject
    private RIBeanManagerUtil beanManagerUtil;

    /**
     * 
     * @return
     */
    public CacheKeyGenerator getKeyGenerator(Class<? extends CacheKeyGenerator> clazz, CacheConfig config) {
        if (config == null) {
            return beanManagerUtil.getBeanByType(clazz);
        } else {
            if (clazz == CacheKeyGenerator.class) {
                return beanManagerUtil.getBeanByType(config.cacheKeyGenerator());
            } else {
                return beanManagerUtil.getBeanByType(clazz);                
            }
        }
    }

    /**
     * 
     * @return
     */
    public CacheResolver getCacheResolver(Class<? extends CacheResolver> clazz, CacheConfig config) {
        if (config == null) {
            return beanManagerUtil.getBeanByType(clazz);
        } else {
            if (clazz == CacheResolver.class) {
                return beanManagerUtil.getBeanByType(config.cacheResolver());
            } else {
                return beanManagerUtil.getBeanByType(clazz);                
            }
        }
    }

    
    /**
     *
     * @param
     * @return
     */
    public String getDefaultMethodCacheName(InvocationContext joinPoint) {
        
        Method method = joinPoint.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();

       StringBuilder cacheName = new StringBuilder(80)
             .append(method.getDeclaringClass().getName())
             .append(".")
             .append(method.getName())
             .append("(");

       for (Class<?> paramType : parameterTypes) {
          cacheName.append(paramType.getName()).append(",");
       }
       if (parameterTypes.length > 0) {
           cacheName.setCharAt(cacheName.length() - 1, ')');
           return cacheName.toString();
       } else {
           return cacheName.append(')').toString();
       }
    }


    /**
     * 
     * @param config
     * @param cacheName
     * @return
     */
    public String findCacheName(CacheConfig config, String cacheName) {
        return cacheName.trim().equals("") && config != null ? config.cacheName() : cacheName;
    }

}
