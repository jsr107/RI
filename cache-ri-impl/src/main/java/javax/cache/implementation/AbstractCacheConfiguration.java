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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The reference implementation for JSR107.
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 * @author Greg Luck
 */
abstract class AbstractCacheConfiguration implements CacheConfiguration {
    private final boolean readThrough;
    private final boolean writeThrough;
    private final AtomicBoolean statisticsEnabled;
    private final CacheConfiguration.Duration[] timeToLive;

    /**
     * Constructor
     * @param writeThrough
     * @param readThrough
     * @param statisticsEnabled
     * @param timeToLive
     */
    AbstractCacheConfiguration(boolean writeThrough, boolean readThrough, boolean statisticsEnabled, CacheConfiguration.Duration[] timeToLive) {
        this.writeThrough = writeThrough;
        this.readThrough = readThrough;
        this.statisticsEnabled = new AtomicBoolean(statisticsEnabled);
        this.timeToLive = timeToLive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadThrough() {
        return readThrough;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteThrough() {
        return writeThrough;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStatisticsEnabled() {
        return statisticsEnabled.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatisticsEnabled(boolean enableStatistics) {
        statisticsEnabled.set(enableStatistics);
    }

    @Override
    public Duration getExpiry(ExpiryType type) {
        return timeToLive[type.ordinal()];
    }

    @Override
    public int hashCode() {
        int result = (readThrough ? 1 : 0);
        result = 31 * result + (writeThrough ? 1 : 0);
        result = 31 * result + (isStatisticsEnabled() ? 1 : 0);
        result = 31 * result + Arrays.hashCode(timeToLive);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheConfiguration)) return false;

        CacheConfiguration that = (CacheConfiguration) o;

        if (isReadThrough() != isReadThrough()) return false;
        if (isStatisticsEnabled() != that.isStatisticsEnabled()) return false;
        for (ExpiryType ttyType : ExpiryType.values()) {
            if (getExpiry(ttyType) != that.getExpiry(ttyType)) return false;
        }
        if (isWriteThrough() != that.isWriteThrough()) return false;

        return true;
    }
}
