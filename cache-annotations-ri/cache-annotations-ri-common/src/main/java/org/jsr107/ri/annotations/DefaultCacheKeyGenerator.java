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
package org.jsr107.ri.annotations;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;
import java.lang.annotation.Annotation;

/**
 * Creates a {@link DefaultGeneratedCacheKey} for the {@link CacheKeyInvocationContext}
 * 
 * @author Eric Dalquist
 * @author Rick Hightower
 * @since 1.0
 */
public class DefaultCacheKeyGenerator implements CacheKeyGenerator {

    /* (non-Javadoc)
     * @see javax.cache.annotation.CacheKeyGenerator#generateCacheKey(javax.cache.annotation.CacheInvocationContext)
     */
    @Override
    public GeneratedCacheKey generateCacheKey(CacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext) {
        final CacheInvocationParameter[] keyParameters = cacheKeyInvocationContext.getKeyParameters();
        
        final Object[] parameters = new Object[keyParameters.length];
        for (int index = 0; index < keyParameters.length; index++) {
            parameters[index] = keyParameters[index].getValue();
        }
        
        return new DefaultGeneratedCacheKey(parameters);
    }
}
