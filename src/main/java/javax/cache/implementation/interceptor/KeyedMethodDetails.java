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
import java.util.Set;

import javax.cache.Cache;
import javax.cache.interceptor.CacheKeyGenerator;


/**
 * Details common to all annotated methods that generate a cache key
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
abstract class KeyedMethodDetails extends MethodDetails {
    private final CacheKeyGenerator cacheKeyGenerator;
    private final List<CacheParameterDetails> allParameters;
    private final List<CacheParameterDetails> keyParameters;

    /**
     * @param cache The cache to use
     * @param methodAnotations All annotations that exist on the method
     * @param cacheKeyGenerator The key generator to use
     * @param allParameters All parameter details
     * @param keyParameters Parameter details to use for key generation
     */
    public KeyedMethodDetails(
            Cache<Object, Object> cache, Set<Annotation> methodAnotations,
            CacheKeyGenerator cacheKeyGenerator,
            List<CacheParameterDetails> allParameters,
            List<CacheParameterDetails> keyParameters) {
        
        super(cache, methodAnotations);
        
        if (cacheKeyGenerator == null) {
            throw new IllegalArgumentException("cacheKeyGenerator cannot be null");
        }
        if (allParameters == null) {
            throw new IllegalArgumentException("allParameters cannot be null");
        }
        if (keyParameters == null) {
            throw new IllegalArgumentException("keyParameters cannot be null");
        }
        
        this.cacheKeyGenerator = cacheKeyGenerator;
        this.allParameters = allParameters;
        this.keyParameters = keyParameters;
    }
    
    /**
     * @return the cacheKeyGenerator
     */
    public CacheKeyGenerator getCacheKeyGenerator() {
        return this.cacheKeyGenerator;
    }

    /**
     * @return the allParameters
     */
    public List<CacheParameterDetails> getAllParameters() {
        return this.allParameters;
    }

    /**
     * @return the keyParameters
     */
    public List<CacheParameterDetails> getKeyParameters() {
        return this.keyParameters;
    }
}
