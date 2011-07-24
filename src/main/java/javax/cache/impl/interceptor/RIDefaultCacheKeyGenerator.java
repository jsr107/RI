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
import java.util.ArrayList;
import java.util.List;

import javax.cache.interceptor.CacheKey;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheKeyParam;
import javax.interceptor.InvocationContext;

/**
 * Creates a {@link RIDefaultCacheKey} for the {@link InvocationContext}.
 *
 * @author Eric Dalquist
 * @author Rick Hightower
 * @since 1.7
 */
public class RIDefaultCacheKeyGenerator implements CacheKeyGenerator {

    /**
     *
     * @see javax.cache.interceptor.CacheKeyGenerator#generateCacheKey(javax.interceptor.InvocationContext)
     */
    public CacheKey generateCacheKey(InvocationContext invocationContext) {
        final Object[] parameters = invocationContext.getParameters();
        Annotation[][] parameterAnnotations = invocationContext.getMethod().getParameterAnnotations();
        List<Object> keyParams = null;
        boolean foundKeyParams = false;
        
        int index = 0;
        for (Annotation[] paramAnnotations : parameterAnnotations) {
            for (Annotation ann : paramAnnotations) {
                if (ann.annotationType() == CacheKeyParam.class) {
                    foundKeyParams = true;
                    /* Lazy initialize the keyParams. */
                    if (keyParams == null) {
                        keyParams = new ArrayList<Object>();
                    }
                    keyParams.add(parameters[index]);
                }
            }
            index++;
        }
        
        if (!foundKeyParams) {
            return new RIDefaultCacheKey(parameters);            
        } else {
            return new RIDefaultCacheKey(keyParams.toArray(new Object[keyParams.size()]));                        
        }
    }

}
