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
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheManager;
import javax.cache.CacheWriter;
import javax.cache.Caching;
import java.util.Set;
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
abstract class AbstractCache<K, V> implements Cache<K, V> {
    private static final int CACHE_LOADER_THREADS = 2;

    private final String cacheName;
    private final String cacheManagerName;
    private final ClassLoader classLoader;
    private final CacheConfiguration configuration;
    private final CacheLoader<K, V> cacheLoader;
    private final CacheWriter<K, V> cacheWriter;
    private final Set<Class<?>> immutableClasses;
    private final ExecutorService executorService = Executors.newFixedThreadPool(CACHE_LOADER_THREADS);


    /**
     * Constructs a cache.
     *
     * @param cacheName        the cache name
     * @param classLoader      the class loader
     * @param cacheManagerName the cache manager name
     * @param immutableClasses the set of immutable classes
     * @param configuration    the configuration
     * @param cacheLoader      the cache loader
     * @param cacheWriter      the cache writer
     */
    AbstractCache(String cacheName, String cacheManagerName, Set<Class<?>> immutableClasses, ClassLoader classLoader,
                  CacheConfiguration configuration,
                  CacheLoader<K, V> cacheLoader, CacheWriter<K, V> cacheWriter) {
        assert configuration != null;
        this.configuration = configuration;

        assert cacheName != null;
        this.cacheName = cacheName;

        assert cacheManagerName != null;
        this.cacheManagerName = cacheManagerName;

        assert immutableClasses != null;
        this.immutableClasses = immutableClasses;

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
    public CacheConfiguration getConfiguration() {
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
    protected CacheLoader<K, V> getCacheLoader() {
        return cacheLoader;
    }

    /**
     * Getter
     * @return the cache writer
     */
    protected CacheWriter<K, V> getCacheWriter() {
        return cacheWriter;
    }

    /**
     * Submit tast to executor
     * @param task task
     */
    protected void submit(FutureTask<?> task) {
        executorService.submit(task);
    }
}
