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
import java.util.HashMap;
import java.util.Map;

import javax.cache.interceptor.CachingDefaults;
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
     */
    private Map<Method, MethodDetails> methodDetails = new HashMap<Method, RICacheLookupUtil.MethodDetails>();

    /**
     * 
     * @author Rick Hightower
     * 
     */
    private static final class MethodDetails {
        private String cacheName;
        private CacheResolver cacheResolver;
        private CacheKeyGenerator cacheKeyGenerator;
        private MethodDetails(){}
    }

    /**
     * 
     */
    private CachingDefaults config;

    /**
     * 
     * @param joinPoint
     * @return
     */
    public CachingDefaults cachingDefaults(InvocationContext joinPoint) {
        if (config == null) {
            config = joinPoint.getTarget().getClass()
                    .getAnnotation(CachingDefaults.class);
        }
        return config;
    }

    /**
     * 
     * @return
     */
    public CacheKeyGenerator getKeyGenerator(
            Class<? extends CacheKeyGenerator> clazz, CachingDefaults config,
            Method method) {
        
        MethodDetails methodDetail = methodDetail(method);
        
        if (methodDetail.cacheKeyGenerator == null)
            if (config == null) {
                methodDetail.cacheKeyGenerator = beanManagerUtil.getBeanByType(clazz);
            } else {
                if (clazz == CacheKeyGenerator.class) {
                    methodDetail.cacheKeyGenerator = beanManagerUtil.getBeanByType(config
                            .cacheKeyGenerator());
                } else {
                    methodDetail.cacheKeyGenerator = beanManagerUtil.getBeanByType(clazz);
                }
        }
        
        return methodDetail.cacheKeyGenerator;
    }

    private MethodDetails methodDetail(Method method) {
        MethodDetails methodDetail = this.methodDetails.get(method);
        if (methodDetail == null) {
            methodDetail = new MethodDetails();
            this.methodDetails.put(method, methodDetail);
        }
        return methodDetail;
    }

    /**
     * 
     * @return
     */
    public CacheResolver getCacheResolver(Class<? extends CacheResolver> clazz,
            CachingDefaults config, Method mehtod) {

        MethodDetails methodDetail = methodDetail(mehtod);
        
        if (config == null) {
            methodDetail.cacheResolver = beanManagerUtil.getBeanByType(clazz);
        } else {
            if (clazz == CacheResolver.class) {
                methodDetail.cacheResolver = beanManagerUtil.getBeanByType(config.cacheResolver());
            } else {
                methodDetail.cacheResolver = beanManagerUtil.getBeanByType(clazz);
            }
        }
        return methodDetail.cacheResolver;

    }

    /**
     * 
     * @param
     * @return
     */
    public String getDefaultMethodCacheName(Method method) {

        Class<?>[] parameterTypes = method.getParameterTypes();

        StringBuilder cacheName = new StringBuilder(80)
                .append(method.getDeclaringClass().getName()).append(".")
                .append(method.getName()).append("(");

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
    public String findCacheName(CachingDefaults config, String cacheName, Method method, boolean generate) {
        MethodDetails methodDetail = methodDetail(method);
        
        if (methodDetail.cacheName == null) {
            methodDetail.cacheName = cacheName.trim().equals("") && config != null ? config
                .cacheName() : cacheName;

            if (generate) {
                methodDetail.cacheName = methodDetail.cacheName.trim().equals("") ? this.getDefaultMethodCacheName(method) : methodDetail.cacheName;
            }

        }
        
        return methodDetail.cacheName;
    }

}
