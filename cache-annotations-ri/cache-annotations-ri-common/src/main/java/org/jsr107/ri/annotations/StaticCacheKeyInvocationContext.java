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

import java.lang.annotation.Annotation;
import java.util.List;

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;


/**
 * Details common to all annotated methods that generate a cache key
 * 
 * @author Eric Dalquist
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.annotation.CacheResult},
 * {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemoveEntry}, or
 * {@link javax.cache.annotation.CacheRemoveAll}.
 * @since 1.0
 */
public abstract class StaticCacheKeyInvocationContext<A extends Annotation> extends AbstractStaticCacheInvocationContext<A> {
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
