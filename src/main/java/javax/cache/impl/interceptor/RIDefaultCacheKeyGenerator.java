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
     */
    private List<KeyDetail> keyParams;
    /**
     * 
     */
    private boolean foundKeyParams;
    
    /**
     * 
     */
    RIDefaultCacheKeyGenerator() {
        
    }

    /**
     * 
     * @author Rick Hightower
     * 
     */
    private static final class KeyDetail {
        private int index;

        private KeyDetail(int index) {
            this.index = index;
        }
    }


    /**
     * 
     * @see javax.cache.interceptor.CacheKeyGenerator#generateCacheKey(javax.interceptor.InvocationContext)
     */
    public CacheKey generateCacheKey(InvocationContext invocationContext) {
        final Object[] parameters = invocationContext.getParameters();

        List<KeyDetail> keyParameters = keyParams(invocationContext);


        if (!foundKeyParams) {
            return new RIDefaultCacheKey(parameters);
        } else {
            List<Object> params = new ArrayList<Object>();
            for (KeyDetail detail : keyParameters) {
                params.add(parameters[detail.index]);
            }
            return new RIDefaultCacheKey(params.toArray(new Object[keyParameters
                    .size()]));
        }
    }


    private List<KeyDetail> keyParams(InvocationContext invocationContext) {
        if (keyParams == null) {
            Annotation[][] parameterAnnotations = invocationContext.getMethod()
            .getParameterAnnotations();

            keyParams = new ArrayList<RIDefaultCacheKeyGenerator.KeyDetail>();
            int index = 0;
            for (Annotation[] paramAnnotations : parameterAnnotations) {
                cacheKeySearch: for (Annotation ann : paramAnnotations) {
                    if (ann.annotationType() == CacheKeyParam.class) {
                        foundKeyParams = true;
                        keyParams.add(new KeyDetail(index));
                        break cacheKeySearch;
                    }
                }
                index++;
            }
        }
        return this.keyParams;
    }

}
