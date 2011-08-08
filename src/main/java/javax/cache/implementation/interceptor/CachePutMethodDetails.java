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
import javax.cache.interceptor.CachePut;

/**
 * Details for a method annotated with {@link CachePut}
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
class CachePutMethodDetails extends KeyedMethodDetails {
    private final CachePut cachePutAnnotation;
    private final CacheParameterDetails cacheValueParameter;

    /**
     * @param cache The cache to use
     * @param methodAnotations All annotations that exist on the method
     * @param cacheKeyGenerator The key generator to use
     * @param allParameters All parameter details
     * @param keyParameters Parameter details to use for key generation
     * @param cacheValueParameter The parameter to store in the cache
     * @param cachePutAnnotation The annotation
     */
    public CachePutMethodDetails(Cache<Object, Object> cache, Set<Annotation> methodAnotations,
            CacheKeyGenerator cacheKeyGenerator, 
            List<CacheParameterDetails> allParameters,
            List<CacheParameterDetails> keyParameters,
            CacheParameterDetails cacheValueParameter,
            CachePut cachePutAnnotation) {
        
        super(cache, methodAnotations, cacheKeyGenerator, allParameters, keyParameters);
        
        if (cacheValueParameter == null) {
            throw new IllegalArgumentException("cacheValueParameter cannot be null");
        }
        if (cachePutAnnotation == null) {
            throw new IllegalArgumentException("cachePutAnnotation cannot be null");
        }
        
        this.cacheValueParameter = cacheValueParameter;
        this.cachePutAnnotation = cachePutAnnotation;
    }



    /* (non-Javadoc)
     * @see javax.cache.implementation.interceptor.MethodDetails#getInterceptorType()
     */
    @Override
    public InterceptorType getInterceptorType() {
        return InterceptorType.CACHE_PUT;
    }

    /**
     * @return the cacheValueParameter
     */
    public CacheParameterDetails getCacheValueParameter() {
        return this.cacheValueParameter;
    }

    /**
     * @return the cachePutAnnotation
     */
    public CachePut getCachePutAnnotation() {
        return this.cachePutAnnotation;
    }
}
