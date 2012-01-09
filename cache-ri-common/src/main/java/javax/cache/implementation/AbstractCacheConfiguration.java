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
import javax.cache.Caching;
import javax.cache.InvalidConfigurationException;
import javax.cache.OptionalFeature;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The reference implementation for JSR107.
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public abstract class AbstractCacheConfiguration implements CacheConfiguration {
    private final boolean readThrough;
    private final boolean writeThrough;
    private final boolean storeByValue;
    private final AtomicBoolean statisticsEnabled;
    private volatile IsolationLevel isolationLevel;
    private volatile Mode transactionMode;
    private final CacheConfiguration.Duration[] timeToLive;

    /**
     * Constructor
     * @param writeThrough
     * @param readThrough
     * @param statisticsEnabled
     * @param timeToLive
     */
    public AbstractCacheConfiguration(boolean readThrough, boolean writeThrough,
                                      boolean storeByValue, boolean statisticsEnabled,
                                      IsolationLevel isolationLevel, Mode transactionMode,
                                      Duration[] timeToLive) {
        this.readThrough = readThrough;
        this.writeThrough = writeThrough;
        this.storeByValue = storeByValue;
        this.statisticsEnabled = new AtomicBoolean(statisticsEnabled);
        this.isolationLevel = isolationLevel;
        this.transactionMode = transactionMode;
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
    public boolean isStoreByValue() {
        return storeByValue;
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


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransactionEnabled() {
        return isolationLevel != null && transactionMode != null;
    }

    @Override
    public IsolationLevel getTransactionIsolationLevel() {
        return isolationLevel;
    }

    @Override
    public Mode getTransactionMode() {
        return transactionMode;
    }

    @Override
    public int hashCode() {
        int result = (readThrough ? 1 : 0);
        result = 31 * result + (writeThrough ? 1 : 0);
        result = 31 * result + (isStatisticsEnabled() ? 1 : 0);
        result = 31 * result + Arrays.hashCode(timeToLive);
        result = 31 * result + (storeByValue ? 1 : 0);
        result = 31 * result + (isolationLevel != null ? isolationLevel.hashCode() : 0);
        result = 31 * result + (transactionMode != null ? transactionMode.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheConfiguration)) return false;

        CacheConfiguration that = (CacheConfiguration) o;

        if (readThrough != that.isReadThrough()) return false;
        if (writeThrough != that.isWriteThrough()) return false;
        if (isStatisticsEnabled() != that.isStatisticsEnabled()) return false;
        for (ExpiryType ttyType : ExpiryType.values()) {
            if (getExpiry(ttyType) != that.getExpiry(ttyType)) return false;
        }
        if (storeByValue != isStoreByValue()) return false;
        if (isolationLevel != that.getTransactionIsolationLevel()) return false;
        if (transactionMode != that.getTransactionMode()) return false;

        return true;
    }

    /**
     * Builds the config
     * @author Yannis Cosmadopoulos
     */
    public abstract static class Builder {
        private static final boolean DEFAULT_READ_THROUGH = false;
        private static final boolean DEFAULT_WRITE_THROUGH = false;
        private static final boolean DEFAULT_STATISTICS_ENABLED = false;
        private static final Duration DEFAULT_TIME_TO_LIVE = Duration.ETERNAL;
        private static final boolean DEFAULT_STORE_BY_VALUE = true;
        private static final IsolationLevel DEFAULT_TRANSACTION_ISOLATION_LEVEL = IsolationLevel.NONE;
        private static final Mode DEFAULT_TRANSACTION_MODE = Mode.NONE;

        /**
         * read through
         */
        protected boolean readThrough = DEFAULT_READ_THROUGH;
        /**
         * write through
         */
        protected boolean writeThrough = DEFAULT_WRITE_THROUGH;
        /**
         * statistics enabled
         */
        protected boolean statisticsEnabled = DEFAULT_STATISTICS_ENABLED;
        /**
         * duration
         */
        protected final Duration[] timeToLive;

        /**
         * store by value
         */
        protected boolean storeByValue = DEFAULT_STORE_BY_VALUE;

        /**
         * isolation level
         */
        protected IsolationLevel isolationLevel = DEFAULT_TRANSACTION_ISOLATION_LEVEL;

        /**
         * transaction mode
         */
        protected Mode transactionMode = DEFAULT_TRANSACTION_MODE;

        /**
         * Constructor
         */
        public Builder() {
            timeToLive = new Duration[ExpiryType.values().length];
            for (int i = 0; i < timeToLive.length; i++) {
                timeToLive[i] = DEFAULT_TIME_TO_LIVE;
            }
        }

        /**
         * the build method
         * @return a configuration
         */
        public abstract CacheConfiguration build();

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
         * Set whether statistics are enabled
         *
         * @param statisticsEnabled statistics are enabled
         * @return this Builder instance
         */
        public Builder setStatisticsEnabled(boolean statisticsEnabled) {
            this.statisticsEnabled = statisticsEnabled;
            return this;
        }

        /**
         * Set expiry
         * @param type ttl type
         * @param duration time to live
         * @return this Builder instance
         */
        public Builder setExpiry(ExpiryType type, Duration duration) {
            if (type == null) {
                throw new NullPointerException();
            }
            if (duration == null) {
                throw new NullPointerException();
            }
            this.timeToLive[type.ordinal()] =
                    duration.getDurationAmount() == 0 ? Duration.ETERNAL : duration;
            return this;
        }


        /**
         * Set whether store by value is active
         *
         * @param storeByValue whether store by value is active
         * @return this Builder instance
         */
        public Builder setStoreByValue(boolean storeByValue) {
            if (!storeByValue && !Caching.isSupported(OptionalFeature.STORE_BY_REFERENCE)) {
                throw new InvalidConfigurationException("storeByValue");
            }
            this.storeByValue = storeByValue;
            return this;
        }

        /**
         * Set whether transactions are enabled
         *
         * @param isolationLevel isolation level
         * @param mode the transactionMode
         * @return this Builder instance
         */
        public Builder setTransactionEnabled(IsolationLevel isolationLevel, Mode mode) {
            if (!Caching.isSupported(OptionalFeature.TRANSACTIONS)) {
                throw new InvalidConfigurationException("transactionsEnabled");
            }
            this.isolationLevel = isolationLevel;
            this.transactionMode = mode;
            return this;
        }
    }
}
