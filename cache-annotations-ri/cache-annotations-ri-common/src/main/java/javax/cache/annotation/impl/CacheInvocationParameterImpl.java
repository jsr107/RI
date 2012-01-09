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
import java.util.Set;

import javax.cache.annotation.CacheInvocationParameter;

/**
 * Provides caching specific information about a method parameter for intercepted method invocations.
 * 
 * @author Eric Dalquist
 * @since 1.0
 */
public class CacheInvocationParameterImpl implements CacheInvocationParameter {
    private final CacheParameterDetails cacheParameterDetails;
    private final Object value;
    
    /**
     * Creates a CacheInvocationParameterImpl
     * 
     * @param cacheParameterDetails The pre-processed details of the parameter
     * @param value The parameter value from the intercepted invocation
     */
    public CacheInvocationParameterImpl(CacheParameterDetails cacheParameterDetails, Object value) {
        this.cacheParameterDetails = cacheParameterDetails;
        this.value = value;
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.CacheInvocationParameter#getRawType()
     */
    @Override
    public Class<?> getRawType() {
        return this.cacheParameterDetails.getRawType();
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.CacheInvocationParameter#getValue()
     */
    @Override
    public Object getValue() {
        return this.value;
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.CacheInvocationParameter#getAnnotations()
     */
    @Override
    public Set<Annotation> getAnnotations() {
        return this.cacheParameterDetails.getAnnotations();
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.CacheInvocationParameter#getParameterPosition()
     */
    @Override
    public int getParameterPosition() {
        return this.cacheParameterDetails.getParameterPosition();
    }

}
