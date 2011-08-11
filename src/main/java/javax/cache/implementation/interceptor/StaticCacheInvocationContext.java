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
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.cache.interceptor.CacheMethodDetails;
import javax.cache.interceptor.CacheResolver;

/**
 * @author Eric Dalquist
 * @version $Revision$
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.interceptor.CacheResult}, 
 * {@link javax.cache.interceptor.CachePut}, {@link javax.cache.interceptor.CacheRemoveEntry}, or 
 * {@link javax.cache.interceptor.CacheRemoveAll}.
 */
abstract class StaticCacheInvocationContext<A extends Annotation> implements CacheMethodDetails<A> {
    private final CacheMethodDetails<A> cacheMethodDetails;
    private final CacheResolver cacheResolver;
    private final List<CacheParameterDetails> allParameters;

    /**
     * @param cacheMethodDetails
     * @param cacheResolver
     * @param allParameters All parameter details
     */
    public StaticCacheInvocationContext(CacheMethodDetails<A> cacheMethodDetails, CacheResolver cacheResolver,
            List<CacheParameterDetails> allParameters) {
        
        if (cacheMethodDetails == null) {
            throw new IllegalArgumentException("cacheMethodDetails cannot be null");
        }
        if (cacheResolver == null) {
            throw new IllegalArgumentException("cacheResolver cannot be null");
        }
        if (allParameters == null) {
            throw new IllegalArgumentException("allParameters cannot be null");
        }
        
        this.cacheMethodDetails = cacheMethodDetails;
        this.cacheResolver = cacheResolver;
        this.allParameters = allParameters;
    }

    /**
     * @return The type of intercepter this {@link CacheMethodDetailsImpl} is for
     */
    public abstract InterceptorType getInterceptorType();

    /**
     * @return the allParameters
     */
    public List<CacheParameterDetails> getAllParameters() {
        return this.allParameters;
    }

    /**
     * @return The {@link CacheResolver} to use to get the cache for this method
     */
    public CacheResolver getCacheResolver() {
        return this.cacheResolver;
    }

    @Override
    public Method getMethod() {
        return this.cacheMethodDetails.getMethod();
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return this.cacheMethodDetails.getAnnotations();
    }
    
    @Override
    public A getCacheAnnotation() {
        return this.cacheMethodDetails.getCacheAnnotation();
    }

    @Override
    public String getCacheName() {
        return this.cacheMethodDetails.getCacheName();
    }
}
