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

import javax.cache.CacheConfiguration;

/**
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public final class RICacheConfiguration implements CacheConfiguration {
    private final String cacheName;
    private boolean readThrough;
    private boolean writeThrough;
    private boolean storeByValue;

    private RICacheConfiguration(String cacheName, boolean readThrough, boolean writeThrough, boolean storeByValue) {
        if (cacheName == null) {
            throw new NullPointerException("cacheName");
        }
        this.cacheName = cacheName;
        setReadThrough(readThrough);
        setWriteThrough(writeThrough);
        setStoreByValue(storeByValue);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadThrough() {
        return readThrough;
    }

    /**
     * {@inheritDoc}
     */
    public void setReadThrough(boolean readThrough) {
        this.readThrough = readThrough;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isWriteThrough() {
        return writeThrough;
    }

    /**
     * {@inheritDoc}
     */
    public void setWriteThrough(boolean writeThrough) {
        this.writeThrough = writeThrough;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStoreByValue() {
        return storeByValue;
    }

    /**
     * {@inheritDoc}
     */
    public void setStoreByValue(boolean storeByValue) {
        this.storeByValue = storeByValue;
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * Builds the config
     * @author Yannis Cosmadopoulos
     */
    public static class Builder {
        private String cacheName;
        private boolean readThrough;
        private boolean writeThrough;
        private boolean storeByValue;

        /**
         * Set whether read through is active
         * @param readThrough whether read through is active
         * @return this Builder instance
         */
        public Builder setReadThrough(boolean readThrough) {
            this.readThrough = readThrough;
            return this;
        }

        /**
         * Set whether write through is active
         *
         * @param writeThrough whether write through is active
         * @return this Builder instance
         */
        public Builder setWriteThrough(boolean writeThrough) {
            this.writeThrough = writeThrough;
            return this;
        }

        /**
         * Set whether store by value is active
         *
         * @param storeByValue whether store by value is active
         * @return this Builder instance
         */
        public Builder setStoreByValue(boolean storeByValue) {
            this.storeByValue = storeByValue;
            return this;
        }

        /**
         * Set the cache name
         * @param cacheName the cache name
         * @return this Builder instance
         */
        public Builder setCacheName(String cacheName) {
            if (cacheName == null) {
                throw new NullPointerException("cacheName");
            }
            this.cacheName = cacheName;
            return this;
        }

        /**
         * Create a new RICacheConfiguration instance.
         *
         * @return a new RICacheConfiguration instance
         */
        public RICacheConfiguration build() {
            return new RICacheConfiguration(cacheName, readThrough, writeThrough, storeByValue);
        }
    }
}
