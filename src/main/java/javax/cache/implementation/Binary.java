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
package javax.cache.implementation;

/**
 * Internal storage.
 * An implementation may be a wrapper of a byte array, an interface to a stream or to a NIO buffer.
 *
 * @param <V> the type of cached values
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public interface Binary<V> {
    /**
     * Get the stored value
     *
     * @return the value
     * @throws javax.cache.CacheException if an error occurred during de-serialization or if binary is not
     * a Binary obtained from a call to keyToBinary of a compatible serializer.
     */
    V get();
}
