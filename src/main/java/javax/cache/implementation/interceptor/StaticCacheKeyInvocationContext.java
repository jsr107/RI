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
import java.util.List;

import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheMethodDetails;
import javax.cache.interceptor.CacheResolver;


/**
 * Details common to all annotated methods that generate a cache key
 * 
 * @author Eric Dalquist
 * @version $Revision$
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.interceptor.CacheResult}, 
 * {@link javax.cache.interceptor.CachePut}, {@link javax.cache.interceptor.CacheRemoveEntry}, or 
 * {@link javax.cache.interceptor.CacheRemoveAll}.
 */
abstract class StaticCacheKeyInvocationContext<A extends Annotation> extends StaticCacheInvocationContext<A> {
    private final CacheKeyGenerator cacheKeyGenerator;
    private final List<CacheParameterDetails> keyParameters;
    
    /**
     * @param cacheMethodDetails
     * @param cacheResolver
     * @param cacheKeyGenerator The key generator to use
     * @param keyParameters Parameter details to use for key generation
     */
    public StaticCacheKeyInvocationContext(CacheMethodDetails<A> cacheMethodDetails, CacheResolver cacheResolver,
            CacheKeyGenerator cacheKeyGenerator, List<CacheParameterDetails> allParameters,
            List<CacheParameterDetails> keyParameters) {
        
        super(cacheMethodDetails, cacheResolver, allParameters);
        
        if (cacheKeyGenerator == null) {
            throw new IllegalArgumentException("cacheKeyGenerator cannot be null");
        }
        if (keyParameters == null) {
            throw new IllegalArgumentException("keyParameters cannot be null");
        }
        
        this.cacheKeyGenerator = cacheKeyGenerator;
        this.keyParameters = keyParameters;
    }

    
    /**
     * @return the cacheKeyGenerator
     */
    public CacheKeyGenerator getCacheKeyGenerator() {
        return this.cacheKeyGenerator;
    }

    /**
     * @return the keyParameters
     */
    public List<CacheParameterDetails> getKeyParameters() {
        return this.keyParameters;
    }
}
