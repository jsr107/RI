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
 * The reference implementation for JSR107.
 * <p/>
 * Simple immutable implementation of {@link CacheConfiguration}
 * <p/>
 * {@inheritDoc}
 *
 * @author Yannis Cosmadopoulos
 */
class RIUnmodifiableCacheConfiguration implements CacheConfiguration {
    private final CacheConfiguration config;

    /**
     * Constructor.
     *
     * @param config the wrapped configuration
     */
    RIUnmodifiableCacheConfiguration(CacheConfiguration config) {
        if (config == null) {
            throw new NullPointerException("config");
        }
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadThrough() {
        return config.isReadThrough();
    }

    /**
     * {@inheritDoc}
     */
    public void setReadThrough(boolean readThrough) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isWriteThrough() {
        return config.isWriteThrough();
    }

    /**
     * {@inheritDoc}
     */
    public void setWriteThrough(boolean writeThrough) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStoreByValue() {
        return config.isStoreByValue();
    }

    /**
     * {@inheritDoc}
     */
    public void setStoreByValue(boolean storeByValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return true if statistics collection is enabled
     */
    public boolean isStatisticsEnabled() {
        return config.isStatisticsEnabled();
    }

    /**
     * Sets whether statistics gathering is set on this cache.
     *
     * @param enableStatistics true fo enable statistics, false to disable
     */
    public void setStatisticsEnabled(boolean enableStatistics) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof CacheConfiguration)) return false;
        return config.equals(o);
    }

    @Override
    public int hashCode() {
        return config.hashCode();
    }
}
