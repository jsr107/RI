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
import javax.cache.InvalidConfigurationException;

/**
 * The reference implementation for JSR107.
 * <p/>
 * Simple immutable implementation of {@link CacheConfiguration}
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 */
class RIWrappedCacheConfiguration implements CacheConfiguration {
    private final CacheConfiguration config;

    /**
     * Constructor.
     *
     * @param config the wrapped configuration
     */
    RIWrappedCacheConfiguration(CacheConfiguration config) {
        if (config == null) {
            throw new NullPointerException("config");
        }
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadThrough() {
        return config.isReadThrough();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadThrough(boolean readThrough) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteThrough() {
        return config.isWriteThrough();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriteThrough(boolean writeThrough) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStoreByValue() {
        return config.isStoreByValue();
    }

    /**
     * @return true if statistics collection is enabled
     */
    @Override
    public boolean isStatisticsEnabled() {
        return config.isStatisticsEnabled();
    }

    /**
     * Sets whether statistics gathering is set on this cache.
     *
     * @param enableStatistics true fo enable statistics, false to disable
     */
    @Override
    public void setStatisticsEnabled(boolean enableStatistics) {
        config.setStatisticsEnabled(enableStatistics);
    }

    /**
     * Checks whether transactions are enabled for this cache.
     * <p/>
     * Default value is false.
     *
     * @return true if statistics collection is enabled
     */
    @Override
    public boolean isTransactionEnabled() {
        return config.isTransactionEnabled();
    }

    @Override
    public void setExpiry(CacheConfiguration.Duration timeToLive) throws InvalidConfigurationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CacheConfiguration.Duration getExpiry() {
        return config.getExpiry();
    }

    @Override
    public void setSize(CacheConfiguration.Size size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CacheConfiguration.Size getSize() {
        return config.getSize();
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
