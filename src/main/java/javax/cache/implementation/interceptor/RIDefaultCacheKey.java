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

import java.util.Arrays;

import javax.cache.interceptor.CacheKey;

/**
 * Default cache key implementation. Keeps a reference to a copy of the entire parameter array from
 * {@link javax.interceptor.InvocationContext#getParameters()} and uses {@link Arrays#deepHashCode(Object[])} to
 * implement {@link #hashCode()} and {@link Arrays#deepEquals(Object[], Object[])} to implement
 * {@link #equals(Object)}
 *
 * @author Eric Dalquist
 * @since 1.7
 */
public class RIDefaultCacheKey implements CacheKey {

    private static final long serialVersionUID = 1L;

    private final Object[] parameters;
    private final int hashCode;

    /**
     * Constructs a default cache key
     *
     * @param parameters the paramters to use
     */
    public RIDefaultCacheKey(Object[] parameters) {
        this.parameters = parameters;
        this.hashCode = Arrays.deepHashCode(parameters);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RIDefaultCacheKey other = (RIDefaultCacheKey) obj;
        if (!Arrays.deepEquals(this.parameters, other.parameters))
            return false;
        return true;
    }
}
