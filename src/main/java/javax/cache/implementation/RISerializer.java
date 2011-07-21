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

import javax.cache.Binary;
import javax.cache.CacheException;
import javax.cache.Serializer;
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
public class RISerializer<V> implements Serializer<V> {
    /**
     * {@inheritDoc}
     */
    public Binary toBinary(V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(value);
            bos.flush();
            return new RIBinary(bos.toByteArray());
        } catch (IOException e) {
            throw new CacheException("RISerializer: " + e.getMessage());
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                // eat this up
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public V fromBinary(Binary binary) {
        if (binary == null) {
            throw new NullPointerException();
        }
        if (!(binary instanceof RIBinary)) {
            throw new CacheException("RISerializer: bad binary type");
        }
        byte[] bytes = ((RIBinary) binary).getBytes();
        ByteArrayInputStream bos = new ByteArrayInputStream(bytes);
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(bos);
            return (V) ois.readObject();
        } catch (IOException e) {
            throw new CacheException("RISerializer: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new CacheException("RISerializer: " + e.getMessage());
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                // eat this up
            }
        }
    }

    /**
     * Binary implementation backed by a byte array.
     */
    private static class RIBinary implements Binary {
        private final byte[] bytes;

        /**
         * Construct a binary holding a byte array.
         * @param bytes the bytes to store
         * @throws NullPointerException if bytes is null
         */
        public RIBinary(byte[] bytes) {
            if (bytes == null) {
                throw new NullPointerException();
            }
            this.bytes = bytes;
        }

        /**
         * Get the bytes stored in the binary.
         *
         * @return the bytes
         */
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RIBinary riBinary = (RIBinary) o;

            return Arrays.equals(bytes, riBinary.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }
}
