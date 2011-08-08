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
import java.util.Set;

import javax.cache.Cache;
import javax.cache.interceptor.CacheRemoveAll;

/**
 * Details for a method annotated with {@link CacheRemoveAll}
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
class CacheRemoveAllMethodDetails extends MethodDetails {
    private final CacheRemoveAll cacheRemoveAllAnnotation;

    /**
     * @param cache The cache to use
     * @param methodAnotations All annotations that exist on the method
     * @param cacheRemoveAllAnnotation the annotation
     */
    public CacheRemoveAllMethodDetails(
            Cache<Object, Object> cache, Set<Annotation> methodAnotations,
            CacheRemoveAll cacheRemoveAllAnnotation) {

        super(cache, methodAnotations);
        
        if (cacheRemoveAllAnnotation == null) {
            throw new IllegalArgumentException("cacheRemoveAllAnnotation cannot be null");
        }
        
        this.cacheRemoveAllAnnotation = cacheRemoveAllAnnotation;
    }

    /* (non-Javadoc)
     * @see javax.cache.implementation.interceptor.MethodDetails#getInterceptorType()
     */
    @Override
    public InterceptorType getInterceptorType() {
        return InterceptorType.CACHE_REMOVE_ALL;
    }

    /**
     * @return the cacheRemoveAllAnnotation
     */
    public CacheRemoveAll getCacheRemoveAllAnnotation() {
        return this.cacheRemoveAllAnnotation;
    }
}
