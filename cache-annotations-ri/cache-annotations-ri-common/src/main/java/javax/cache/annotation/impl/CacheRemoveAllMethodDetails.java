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
package javax.cache.annotation.impl;

import java.util.List;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolver;

/**
 * Details for a method annotated with {@link CacheRemoveAll}
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class CacheRemoveAllMethodDetails extends StaticCacheInvocationContext<CacheRemoveAll> {
    
    /**
     * Create a new details object for {@link CacheRemoveAll}
     * 
     * @param cacheMethodDetails The base details of the annotated method
     * @param cacheResolver The cache resolver to use
     * @param allParameters An immutable list of all parameter details
     */
    public CacheRemoveAllMethodDetails(
            CacheMethodDetails<CacheRemoveAll> cacheMethodDetails,
            CacheResolver cacheResolver, List<CacheParameterDetails> allParameters) {
        super(cacheMethodDetails, cacheResolver, allParameters);
    }

    /* (non-Javadoc)
     * @see javax.cache.implementation.interceptor.MethodDetails#getInterceptorType()
     */
    @Override
    public InterceptorType getInterceptorType() {
        return InterceptorType.CACHE_REMOVE_ALL;
    }
}
