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
 * Tagging interface for a binary representation of a value or key.
 * An implementation may be a wrapper of a byte array, an interface to a stream or to a NIO buffer.
 *
 * @param <T> the type of the object
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
interface Serializer<T> {
    /**
     * Convert an object to a binary.
     *
     * @param value the value
     * @return binary representation of value
     * @throws javax.cache.CacheException is an error occurred during serialization
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if the value can not be stored
     */
    Binary<T> createBinary(T value);
}
