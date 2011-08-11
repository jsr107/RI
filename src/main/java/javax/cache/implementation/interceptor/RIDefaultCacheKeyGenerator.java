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

import java.lang.annotation.Annotation;

import javax.cache.interceptor.CacheInvocationParameter;
import javax.cache.interceptor.CacheKey;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheKeyInvocationContext;

/**
 * Creates a {@link RIDefaultCacheKey} for the {@link javax.interceptor.InvocationContext}.
 * 
 * @author Eric Dalquist
 * @author Rick Hightower
 * @since 1.7
 */
public class RIDefaultCacheKeyGenerator implements CacheKeyGenerator {

    /* (non-Javadoc)
     * @see javax.cache.interceptor.CacheKeyGenerator#generateCacheKey(javax.cache.interceptor.CacheInvocationContext)
     */
    @Override
    public CacheKey generateCacheKey(CacheKeyInvocationContext<Annotation> cacheKeyInvocationContext) {
        final CacheInvocationParameter[] keyParameters = cacheKeyInvocationContext.getKeyParameters();
        
        final Object[] parameters = new Object[keyParameters.length];
        for (int index = 0; index < keyParameters.length; index++) {
            parameters[index] = keyParameters[index].getValue();
        }
        
        return new RIDefaultCacheKey(parameters);
    }
}
