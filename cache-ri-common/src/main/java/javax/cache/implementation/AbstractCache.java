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

import javax.cache.Cache;
import javax.cache.CacheBuilder;
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheManager;
import javax.cache.CacheWriter;
import javax.cache.Caching;
import javax.cache.InvalidConfigurationException;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class to help implementers
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Yannis Cosmadopoulos
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {
    private static final int CACHE_LOADER_THREADS = 2;

    private final String cacheName;
    private final String cacheManagerName;
    private final ClassLoader classLoader;
    private final CacheConfiguration<K, V> configuration;
    private final CacheLoader<K, ? extends V> cacheLoader;
    private final CacheWriter<? super K, ? super V> cacheWriter;
    private final ExecutorService executorService = Executors.newFixedThreadPool(CACHE_LOADER_THREADS);


    /**
     * Constructs a cache.
     *
     * @param cacheName        the cache name
     * @param cacheManagerName the cache manager name
     * @param classLoader      the class loader
     * @param configuration    the configuration
     * @param cacheLoader      the cache loader
     * @param cacheWriter      the cache writer
     */
    public AbstractCache(String cacheName, String cacheManagerName, ClassLoader classLoader,
                  CacheConfiguration<K, V> configuration,
                  CacheLoader<K, ? extends V> cacheLoader, CacheWriter<? super K, ? super V> cacheWriter) {
        assert configuration != null;
        this.configuration = configuration;

        assert cacheName != null;
        this.cacheName = cacheName;

        assert cacheManagerName != null;
        this.cacheManagerName = cacheManagerName;

        assert classLoader != null;
        this.classLoader = classLoader;

        this.cacheWriter = cacheWriter;
        this.cacheLoader = cacheLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return cacheName;
    }

    /**
     * @inheritDoc
     */
    @Override
    public CacheManager getCacheManager() {
        return Caching.getCacheManager(classLoader, cacheManagerName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheConfiguration<K, V> getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Getter
     * @return the cache loader
     */
    protected CacheLoader<K, ? extends V> getCacheLoader() {
        return cacheLoader;
    }

    /**
     * Getter
     * @return the cache writer
     */
    protected CacheWriter<? super K, ? super V> getCacheWriter() {
        return cacheWriter;
    }

    /**
     * Getter
     * @return class loader
     */
    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Submit tast to executor
     * @param task task
     */
    protected void submit(FutureTask<?> task) {
        executorService.submit(task);
    }

    /**
     * Builder
     *
     * @param <K>
     * @param <V>
     * @author Yannis Cosmadopoulos
     */
    public abstract static class Builder<K, V> implements CacheBuilder<K, V> {
        /**
         * cache name
         */
        protected final String cacheName;
        /**
         * cache manager name
         */
        protected final String cacheManagerName;
        /**
         * class loader
         */
        protected final ClassLoader classLoader;
        /**
         * cache loader
         */
        protected CacheLoader<K, ? extends V> cacheLoader;
        /**
         * cache writer
         */
        protected CacheWriter<? super K, ? super V> cacheWriter;


        private final AbstractCacheConfiguration.Builder configurationBuilder;

        /**
         * builder
         * @param cacheName
         * @param cacheManagerName
         * @param classLoader
         */
        public Builder(String cacheName, String cacheManagerName,
                       ClassLoader classLoader,
                       AbstractCacheConfiguration.Builder configurationBuilder) {
            if (cacheName == null) {
                throw new NullPointerException("cacheName");
            }
            this.cacheName = cacheName;
            if (classLoader == null) {
                throw new NullPointerException("cacheLoader");
            }
            this.classLoader = classLoader;
            if (cacheManagerName == null) {
                throw new NullPointerException("cacheManagerName");
            }
            this.cacheManagerName = cacheManagerName;
            if (configurationBuilder == null) {
                throw new NullPointerException("configurationBuilder");
            }
            this.configurationBuilder = configurationBuilder;
        }

        @Override
        public Builder<K, V> setCacheLoader(CacheLoader<K, ? extends V> cacheLoader) {
            if (cacheLoader == null) {
                throw new NullPointerException("cacheLoader");
            }
            this.cacheLoader = cacheLoader;
            return this;
        }

        @Override
        public Builder<K, V> setCacheWriter(CacheWriter<? super K, ? super V> cacheWriter) {
            if (cacheWriter == null) {
                throw new NullPointerException("cacheWriter");
            }
            this.cacheWriter = cacheWriter;
            return this;
        }

        @Override
        public Builder<K, V> setStatisticsEnabled(boolean enableStatistics) {
            configurationBuilder.setStatisticsEnabled(enableStatistics);
            return this;
        }

        @Override
        public Builder<K, V> setReadThrough(boolean readThrough) {
            configurationBuilder.setReadThrough(readThrough);
            return this;
        }

        @Override
        public Builder<K, V> setWriteThrough(boolean writeThrough) {
            configurationBuilder.setWriteThrough(writeThrough);
            return this;
        }

        @Override
        public Builder<K, V> setExpiry(CacheConfiguration.ExpiryType type, CacheConfiguration.Duration duration) {
            if (type == null) {
                throw new NullPointerException();
            }
            if (duration == null) {
                throw new NullPointerException();
            }
            configurationBuilder.setExpiry(type, duration);
            return this;
        }

        @Override
        public Builder<K, V> setStoreByValue(boolean storeByValue) {
            configurationBuilder.setStoreByValue(storeByValue);
            return this;
        }

        @Override
        public Builder<K, V> setTransactionEnabled(IsolationLevel isolationLevel, Mode mode) {
            configurationBuilder.setTransactionEnabled(isolationLevel, mode);
            return this;
        }

        /**
         * create configuration
         * @return a CacheConfiguration
         */
        protected CacheConfiguration<K, V> createCacheConfiguration() {
            CacheConfiguration<K, V> configuration = configurationBuilder.build();
            if (configuration.isReadThrough() && (cacheLoader == null)) {
                throw new InvalidConfigurationException("cacheLoader");
            }
            if (configuration.isWriteThrough() && (cacheWriter == null)) {
                throw new InvalidConfigurationException("cacheWriter");
            }
            return configuration;
        }
    }
}
