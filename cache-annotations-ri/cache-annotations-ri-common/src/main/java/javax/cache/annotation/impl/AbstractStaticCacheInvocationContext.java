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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;

/**
 * @author Eric Dalquist
 * @version $Revision$
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.annotation.CacheResult},
 * {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemoveEntry}, or
 * {@link javax.cache.annotation.CacheRemoveAll}.
 */
public abstract class AbstractStaticCacheInvocationContext<A extends Annotation> implements StaticCacheInvocationContext<A> {
    private final CacheMethodDetails<A> cacheMethodDetails;
    private final CacheResolver cacheResolver;
    private final List<CacheParameterDetails> allParameters;

    /**
     * Create a new static invocation instance
     * 
     * @param cacheMethodDetails Static details about the method
     * @param cacheResolver The cache resolver to use for the method
     * @param allParameters All parameter details
     */
    public AbstractStaticCacheInvocationContext(CacheMethodDetails<A> cacheMethodDetails, CacheResolver cacheResolver,
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
     * @return the allParameters
     */
    @Override
    public List<CacheParameterDetails> getAllParameters() {
        return this.allParameters;
    }

    /**
     * @return The {@link CacheResolver} to use to get the cache for this method
     */
    @Override
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
