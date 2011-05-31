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

    private boolean readThrough;
    private boolean writeThrough;
    private boolean storeByValue;

    private RICacheConfiguration(boolean readThrough, boolean writeThrough, boolean storeByValue) {
        setReadThrough(readThrough);
        setWriteThrough(writeThrough);
        setStoreByValue(storeByValue);
    }

    /**
     * @inheritdoc
     */
    public boolean isReadThrough() {
        return readThrough;
    }

    /**
     * @inheritdoc
     */
    public void setReadThrough(boolean readThrough) {
        this.readThrough = readThrough;
    }

    /**
     * @inheritdoc
     */
    public boolean isWriteThrough() {
        return writeThrough;
    }

    /**
     * @inheritdoc
     */
    public void setWriteThrough(boolean writeThrough) {
        this.writeThrough = writeThrough;
    }

    /**
     * @inheritdoc
     */
    public boolean isStoreByValue() {
        return storeByValue;
    }

    /**
     * @inheritdoc
     */
    public void setStoreByValue(boolean storeByValue) {
        this.storeByValue = storeByValue;
    }

    /**
     * Builds the config
     * @author Yannis Cosmadopoulos
     */
    public static class Builder {
        private boolean readThrough;
        private boolean writeThrough;
        private boolean storeByValue;

        /**
         *
         * @param readThrough
         * @return
         */
        public Builder setReadThrough(boolean readThrough) {
            this.readThrough = readThrough;
            return this;
        }

        /**
         *
         * @param writeThrough
         * @return
         */
        public Builder setWriteThrough(boolean writeThrough) {
            this.writeThrough = writeThrough;
            return this;
        }

        /**
         *
         * @param storeByValue
         * @return
         */
        public Builder setStoreByValue(boolean storeByValue) {
            this.storeByValue = storeByValue;
            return this;
        }

        /**
         *
         * @return
         */
        public RICacheConfiguration build() {
            return new RICacheConfiguration(readThrough, writeThrough, storeByValue);
        }
    }
}
