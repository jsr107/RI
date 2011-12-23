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

import javax.cache.CacheManager;
import javax.cache.CachingShutdownException;
import javax.cache.spi.CacheManagerFactory;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Simple implementation of the Caching object
 * @author ycosmado
 * @since 1.0
 */
public abstract class AbstractCacheManagerFactory implements CacheManagerFactory {
    private final Map<ClassLoader, Map<String, CacheManager>> cacheManagers = new HashMap<ClassLoader, Map<String, CacheManager>>();

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheManager getCacheManager(String name) {
        return getCacheManager(getDefaultClassLoader(), name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheManager getCacheManager(ClassLoader classLoader, String name) {
        if (classLoader == null) {
            throw new NullPointerException("classLoader");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        synchronized (cacheManagers) {
            Map<String, CacheManager> map = cacheManagers.get(classLoader);
            if (map == null) {
                map = new HashMap<String, CacheManager>();
                cacheManagers.put(classLoader, map);
            }
            CacheManager cacheManager = map.get(name);
            if (cacheManager == null) {
                cacheManager = createCacheManager(classLoader, name);
                map.put(name, cacheManager);
            }
            return cacheManager;
        }
    }

    /**
     * Create CacheManager instance
     * @param classLoader the class loader
     * @param name the name
     * @return a CacheManager
     */
    protected abstract CacheManager createCacheManager(ClassLoader classLoader, String name);

    /**
     * Get the classloader
     * @return the classloader
     */
    protected abstract ClassLoader getDefaultClassLoader();

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws CachingShutdownException {
        synchronized (cacheManagers) {
            IdentityHashMap<CacheManager, Exception> failures = new IdentityHashMap<CacheManager, Exception>();
            for (Map<String, CacheManager> cacheManagerMap : cacheManagers.values()) {
                try {
                    shutdown(cacheManagerMap);
                } catch (CachingShutdownException e) {
                    failures.putAll(e.getFailures());
                }
            }
            cacheManagers.clear();
            if (!failures.isEmpty()) {
                throw new CachingShutdownException(failures);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean close(ClassLoader classLoader) throws CachingShutdownException {
        Map<String, CacheManager> cacheManagerMap;
        synchronized (cacheManagers) {
            cacheManagerMap = cacheManagers.remove(classLoader);
        }
        if (cacheManagerMap == null) {
            return false;
        } else {
            shutdown(cacheManagerMap);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean close(ClassLoader classLoader, String name) throws CachingShutdownException {
        CacheManager cacheManager;
        synchronized (cacheManagers) {
            Map<String, CacheManager> cacheManagerMap = cacheManagers.get(classLoader);
            cacheManager = cacheManagerMap.remove(name);
            if (cacheManagerMap.isEmpty()) {
                cacheManagers.remove(classLoader);
            }
        }
        if (cacheManager == null) {
            return false;
        } else {
            cacheManager.shutdown();
            return true;
        }
    }

    private void shutdown(Map<String, CacheManager> cacheManagerMap) throws CachingShutdownException {
        IdentityHashMap<CacheManager, Exception> failures = new IdentityHashMap<CacheManager, Exception>();
        for (CacheManager cacheManager : cacheManagerMap.values()) {
            try {
                cacheManager.shutdown();
            } catch (Exception e) {
                failures.put(cacheManager, e);
            }
        }
        if (!failures.isEmpty()) {
            throw new CachingShutdownException(failures);
        }
    }
}
