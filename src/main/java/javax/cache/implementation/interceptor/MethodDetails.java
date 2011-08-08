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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.cache.Cache;


/**
 * Details common to all annotated methods
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
abstract class MethodDetails {
    private final Cache<Object, Object> cache;
    private final Set<Annotation> methodAnotations;

    /**
     * @param cache The cache to use
     * @param methodAnnotations All annotations that exist on the method
     */
    public MethodDetails(
            Cache<Object, Object> cache, Set<Annotation> methodAnnotations) {
        if (cache == null) {
            throw new IllegalArgumentException("cache cannot be null");
        }
        if (methodAnnotations == null) {
            throw new IllegalArgumentException("methodAnnotations cannot be null");
        }

        this.cache = cache;
        this.methodAnotations = Collections.unmodifiableSet(new LinkedHashSet<Annotation>(methodAnnotations));
    }
    
    /**
     * @return The type of intercepter this {@link MethodDetails} is for
     */
    public abstract InterceptorType getInterceptorType();

    /**
     * @return the cache
     */
    public Cache<Object, Object> getCache() {
        return this.cache;
    }

    /**
     * @return the methodAnotations
     */
    public Set<Annotation> getMethodAnotations() {
        return this.methodAnotations;
    }
}
