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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;

/**
 * The reference implementation for JSR107.
 * <p/>
 * This serializer uses java serialization.
 *
 * @param <V> the type of values
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public class RIByValueSerializer<V> implements Serializer<V> {
    private final SerializationHelper serializationHelper;

    /**
     * Constructor
     * @param classLoader the class loader
     */
    public RIByValueSerializer(ClassLoader classLoader) {
        this.serializationHelper = new SerializationHelper(classLoader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binary<V> createBinary(V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        //TODO: do we want to validate?
        //serializationHelper.validate(value);
        return new RIBinary<V>(serializationHelper, value);
    }

    /**
     * Binary implementation backed by a byte array.
     */
    private static final class RIBinary<V> implements Binary<V> {
        private final byte[] bytes;
        private final int hashCode;
        private final SerializationHelper serializationHelper;

        private RIBinary(SerializationHelper serializationHelper, V value) {
            this.serializationHelper = serializationHelper;
            hashCode = value.hashCode();
            try {
                bytes = serializationHelper.toBytes(value);
            } catch (IOException e) {
                throw new IllegalArgumentException("Serializer: " + e.getMessage(), e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V get() {
            try {
                return (V) serializationHelper.fromBytes(bytes);
            } catch (IOException e) {
                throw new CacheException("Serializer: " + e.getMessage(), e);
            } catch (ClassNotFoundException e) {
                throw new CacheException("Serializer: " + e.getMessage(), e);
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
    }

    /**
     * Simple helper to go to and from byte arrays using a classloader
     */
    private static final class SerializationHelper {
        private final ClassLoader classLoader;

        private SerializationHelper(ClassLoader classLoader) {
            if (classLoader == null) {
                throw new NullPointerException("classLoader");
            }
            this.classLoader = classLoader;
        }

        public byte[] toBytes(Object value) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
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
            }
        }

        public Object fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
            ByteArrayInputStream bos = new ByteArrayInputStream(bytes);
            ObjectInputStream ois;
            try {
                ois = new MyObjectInputStream(bos, classLoader);
                return ois.readObject();
            } finally {
                try {
                    bos.close();
                } catch (IOException e) {
                    // eat this up
                }
            }
        }

        public void validate(Object toStore) {
            Class class1 = toStore.getClass();
            try {
                Class class2 = classLoader.loadClass(class1.getName());
                if (class1 !=  class2) {
                    throw new IllegalArgumentException("from different class loader: " + toStore);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("not in class loader: " + toStore);
            }
        }

        /**
         * want to use our ClassLoader to resolve classes
         */
        private static final class MyObjectInputStream extends ObjectInputStream {
            private final ClassLoader classloader;

            private MyObjectInputStream(InputStream in, ClassLoader classloader) throws IOException {
                super(in);
                this.classloader = classloader;
            }

            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                String name = desc.getName();
                try {
                    return Class.forName(name, false, classloader);
                } catch (ClassNotFoundException ex) {
                    return super.resolveClass(desc);
                }
            }
        }
    }
}
