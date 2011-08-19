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

import javax.cache.CacheException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

/**
 * The reference implementation for JSR107.
 * <p/>
 * This serializer uses java serialization.
 *
 * @param <V> the type of values
 * @author Yannis Cosmadopoulos
 * @since 1.7
 */
public class RIByValueSerializer<V> implements Serializer<V> {
    private final ClassLoader classLoader;

    /**
     * Constructor
     * @param classLoader the class loader
     */
    public RIByValueSerializer(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new NullPointerException("classLoader");
        }
        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary<V> createBinary(V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new RIBinary<V>(classLoader, value);
    }

    /**
     * Binary implementation backed by a byte array.
     */
    private static final class RIBinary<V> implements Binary<V> {
        private final byte[] bytes;
        private final int hashCode;
        private final ClassLoader classLoader;

        private RIBinary(ClassLoader classLoader, V value) {
            this.classLoader = classLoader;
            hashCode = value.hashCode();
            try {
                bytes = toBytes(value);
            } catch (IOException e) {
                throw new CacheException("Serializer: " + e.getMessage());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V get() {
            try {
                return fromBytes(bytes);
            } catch (IOException e) {
                throw new CacheException("Serializer: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new CacheException("Serializer: " + e.getMessage());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RIBinary riBinary = (RIBinary) o;

            return hashCode == riBinary.hashCode &&
                    (Arrays.equals(bytes, riBinary.bytes) || get().equals(riBinary.get()));
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        private byte[] toBytes(V value) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(value);
                bos.flush();
                return bos.toByteArray();
            } finally {
                try {
                    bos.close();
                } catch (IOException e) {
                    // eat this up
                }
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }

        private V fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
            ByteArrayInputStream bos = new ByteArrayInputStream(bytes);
            ObjectInputStream ois;
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                ois = new ObjectInputStream(bos);
                return (V) ois.readObject();
            } finally {
                try {
                    bos.close();
                } catch (IOException e) {
                    // eat this up
                }
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
    }
}
