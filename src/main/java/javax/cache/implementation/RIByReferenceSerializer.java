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

import javax.cache.Serializer;

/**
 * The reference implementation for JSR107.
 * <p/>
 * This serializer stores by reference.
 *
 * @param <V> the type of values
 * @author Yannis Cosmadopoulos
 * @since 1.7
 */
public class RIByReferenceSerializer<V> implements Serializer<V> {
    /**
     * {@inheritDoc}
     */
    public Binary<V> createBinary(V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new RIBinary<V>(value);
    }

    /**
     * Store by reference
     *
     * @param <V>
     */
    private static final class RIBinary<V> implements Binary<V> {
        private final V value;

        private RIBinary(V value) {
            this.value = value;
        }

        public V get() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RIBinary riBinary = (RIBinary) o;

            return value.equals(riBinary.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
