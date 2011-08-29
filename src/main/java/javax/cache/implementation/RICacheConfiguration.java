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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The reference implementation for JSR107.
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 * @author Greg Luck
 */
public final class RICacheConfiguration implements CacheConfiguration {

    private final AtomicBoolean readThrough;
    private final AtomicBoolean writeThrough;
    private final AtomicBoolean storeByValue;
    private final AtomicBoolean statisticsEnabled;
    private final AtomicBoolean transactionsEnabled;


    private RICacheConfiguration(boolean readThrough,
                                 boolean writeThrough,
                                 boolean storeByValue,
                                 boolean statisticsEnabled,
                                 boolean transactionsEnabled) {
        this.readThrough = new AtomicBoolean(readThrough);
        this.writeThrough = new AtomicBoolean(writeThrough);
        this.storeByValue = new AtomicBoolean(storeByValue);
        this.statisticsEnabled = new AtomicBoolean(statisticsEnabled);
        this.transactionsEnabled = new AtomicBoolean(transactionsEnabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadThrough() {
        return readThrough.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadThrough(boolean readThrough) {
        this.readThrough.set(readThrough);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWriteThrough() {
        return writeThrough.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriteThrough(boolean writeThrough) {
        this.writeThrough.set(writeThrough);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStoreByValue() {
        return storeByValue.get();
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
        this.statisticsEnabled.set(enableStatistics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransactionEnabled() {
        return transactionsEnabled.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheConfiguration)) return false;

        CacheConfiguration that = (CacheConfiguration) o;

        if (isReadThrough() != that.isReadThrough()) return false;
        if (isStatisticsEnabled() != that.isStatisticsEnabled()) return false;
        if (isStoreByValue() != that.isStoreByValue()) return false;
        if (isTransactionEnabled() != that.isTransactionEnabled()) return false;
        if (isWriteThrough() != that.isWriteThrough()) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = readThrough.hashCode();
        boolean b;

        b = isWriteThrough();
        result = 31 * result + (b ? 1 : 0);
        b = isStoreByValue();
        result = 31 * result + (b ? 1 : 0);
        b = isStatisticsEnabled();
        result = 31 * result + (b ? 1 : 0);
        b = isTransactionEnabled();
        result = 31 * result + (b ? 1 : 0);
        return result;
    }

    /**
     * Builds the config
     * @author Yannis Cosmadopoulos
     */
    public static class Builder {
        private static final boolean DEFAULT_READ_THROUGH = false;
        private static final boolean DEFAULT_WRITE_THROUGH = false;
        private static final boolean DEFAULT_STORE_BY_VALUE = true;
        private static final boolean DEFAULT_STATISTICS_ENABLED = false;
        private static final boolean DEFAULT_TRANSACTIONS_ENABLED = false;

        private boolean readThrough = DEFAULT_READ_THROUGH;
        private boolean writeThrough = DEFAULT_WRITE_THROUGH;
        private boolean storeByValue = DEFAULT_STORE_BY_VALUE;
        private boolean statisticsEnabled = DEFAULT_STATISTICS_ENABLED;
        private boolean transactionsEnabled = DEFAULT_TRANSACTIONS_ENABLED;

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
         * Set whether transactions are enabled
         *
         * @param transactionsEnabled whether transactions are enabled
         * @return this Builder instance
         */
        public Builder setTransactionEnabled(boolean transactionsEnabled) {
            this.transactionsEnabled = transactionsEnabled;
            return this;
        }

        /**
         * Create a new RICacheConfiguration instance.
         *
         * @return a new RICacheConfiguration instance
         */
        public RICacheConfiguration build() {
            return new RICacheConfiguration(readThrough, writeThrough, storeByValue, statisticsEnabled, transactionsEnabled);
        }
    }
}
