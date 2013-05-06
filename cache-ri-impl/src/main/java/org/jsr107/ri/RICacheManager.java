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

package org.jsr107.ri;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Configuration;
import javax.cache.OptionalFeature;
import javax.cache.spi.CachingProvider;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;
import javax.transaction.UserTransaction;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The reference implementation of the {@link CacheManager}.
 *
 * @author Yannis Cosmadopoulos
 * @author Brian Oliver
 *
 * @since 1.0
 */
public class RICacheManager implements CacheManager {

    private static final Logger LOGGER = Logger.getLogger("javax.cache");
    private final HashMap<String, Cache<?, ?>> caches = new HashMap<String, Cache<?, ?>>();

    private final RICachingProvider cachingProvider;

    private final URI uri;
    private final WeakReference<ClassLoader> classLoaderReference;
    private final Properties properties;

    private volatile boolean isClosed;

    /**
     * Constructs a new RICacheManager with the specified name.
     *
     * @param cachingProvider  the CachingProvider that created the CacheManager
     * @param uri              the name of this cache manager
     * @param classLoader      the ClassLoader that should be used in converting values into Java Objects.
     * @param properties       the vendor specific Properties for the CacheManager
     *
     * @throws NullPointerException if the URI and/or classLoader is null.
     */
    public RICacheManager(RICachingProvider cachingProvider, URI uri, ClassLoader classLoader, Properties properties) {
        if (cachingProvider == null) {
            throw new NullPointerException("No CachingProvider specified");
        }
        this.cachingProvider = cachingProvider;

        if (uri == null) {
            throw new NullPointerException("No CacheManager URI specified");
        }
        this.uri = uri;

        if (classLoader == null) {
            throw new NullPointerException("No ClassLoader specified");
        }
        this.classLoaderReference = new WeakReference<ClassLoader>(classLoader);

        this.properties = properties == null ? new Properties() : new Properties(properties);

        isClosed = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CachingProvider getCachingProvider() {
        return cachingProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        if (!isClosed()) {
            //first releaseCacheManager the CacheManager from the CacheProvider so that
            //future requests for this CacheManager won't return this one
            cachingProvider.releaseCacheManager(getURI(), getClassLoader());

            isClosed = true;

            ArrayList<Cache<?, ?>> cacheList;
            synchronized (caches) {
                cacheList = new ArrayList<Cache<?, ?>>(caches.values());
                caches.clear();
            }
            for (Cache<?, ?> cache : cacheList) {
                try {
                    cache.close();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error stopping cache: " + cache, e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getURI() {
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties getProperties() {
        return properties;
    }

    /**
     * Getter
     * @return the class loader
     */
    protected ClassLoader getClassLoader() {
        return classLoaderReference.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> Cache<K, V> configureCache(String cacheName, Configuration<K, V> configuration) {
        if (isClosed()) {
            throw new IllegalStateException();
        }

        if (cacheName == null) {
            throw new NullPointerException("cacheName must not be null");
        }
        
        if (configuration == null) {
            throw new NullPointerException("configuration must not be null");
        }

        if ((!configuration.isStoreByValue()) && configuration.isTransactionsEnabled()) {
            throw new IllegalArgumentException("can't use store-by-reference and transactions together");
        }

        if (configuration.getTransactionIsolationLevel() == IsolationLevel.NONE &&
            configuration.getTransactionMode() != Mode.NONE) {
            throw new IllegalArgumentException("isolation level expected when mode specified");
        }
        
        if (configuration.getTransactionIsolationLevel() != IsolationLevel.NONE &&
            configuration.getTransactionMode() == Mode.NONE) {
            throw new IllegalArgumentException("mode expected when isolation level specified");
        }

        synchronized (caches) {
            Cache<?, ?> cache = caches.get(cacheName);
            
            if (cache == null) {
                cache = new RICache<K, V>(this, cacheName, getClassLoader(), configuration);
                caches.put(cache.getName(), cache);
            }
        
            return (Cache<K, V>)cache;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        synchronized (caches) {
            /*
             * Can't really verify that the K/V cast is safe but it is required by the API, using a 
             * local variable for the cast to allow for a minimal scoping of @SuppressWarnings 
             */
            @SuppressWarnings("unchecked")
            final Cache<K, V> cache = (Cache<K, V>) caches.get(cacheName);
            return cache;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Cache<?, ?>> getCaches() {
        synchronized (caches) {
            HashSet<Cache<?, ?>> set = new HashSet<Cache<?, ?>>();
            for (Cache<?, ?> cache : caches.values()) {
                set.add(cache);
            }
            return Collections.unmodifiableSet(set);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeCache(String cacheName) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException();
        }

        Cache<?, ?> cache = getCache(cacheName);

        if (cache == null) {
            return false;
        } else {
            cache.close();
            return true;
        }
    }

    /**
     * Releases the Cache with the specified name from being managed by
     * this CacheManager.
     *
     * @param cacheName  the name of the Cache to releaseCacheManager
     */
    void releaseCache(String cacheName) {
        if (cacheName == null) {
            throw new NullPointerException();
        }
        synchronized (caches) {
            caches.remove(cacheName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserTransaction getUserTransaction() {
        throw new UnsupportedOperationException("Transactions are not supported.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(OptionalFeature optionalFeature) {
        return getCachingProvider().isSupported(optionalFeature);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableStatistics(String cacheName, boolean enabled) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException();
        }
        ((RICache)caches.get(cacheName)).setStatisticsEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableManagement(String cacheName, boolean enabled) {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        if (cacheName == null) {
            throw new NullPointerException();
        }
        ((RICache)caches.get(cacheName)).setManagementEnabled(enabled);
    }

    @Override
    public <T> T unwrap(java.lang.Class<T> cls) {
        if (cls.isAssignableFrom(getClass())) {
            return cls.cast(this);
        }

        throw new IllegalArgumentException("Unwapping to " + cls + " is not a supported by this implementation");
    }

    /**
     * Obtain the logger.
     *
     * @return the logger.
     */
    Logger getLogger() {
        return LOGGER;
    }
}
