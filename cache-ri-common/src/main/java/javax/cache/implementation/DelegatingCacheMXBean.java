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
import javax.cache.CacheException;
import javax.cache.mbeans.CacheMXBean;
import javax.cache.Status;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Date;

/**
 * Class to help implementers
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Yannis Cosmadopoulos
 */
public class DelegatingCacheMXBean<K, V> implements CacheMXBean {
    private final Cache<K, V> cache;

    /**
     * Constructor
     * @param cache the cache
     */
    public DelegatingCacheMXBean(Cache<K, V> cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return cache.getName();
    }

    @Override
    public Status getStatus() {
        return cache.getStatus();
    }

    @Override
    public ObjectName getObjectName() {
        try {
            return new ObjectName("javax.cache:type=Cache" +
                    ",CacheManager=" + mbeanSafe(cache.getCacheManager().getName()) +
                    ",name=" + mbeanSafe(cache.getName()));
        } catch (MalformedObjectNameException e) {
            throw new CacheException(e);
        }
    }

    @Override
    public void clearStatistics() {
        cache.getStatistics().clearStatistics();
    }

    @Override
    public Date statsAccumulatingFrom() {
        return cache.getStatistics().statsAccumulatingFrom();
    }

    @Override
    public long getCacheHits() {
        return cache.getStatistics().getCacheHits();
    }

    @Override
    public float getCacheHitPercentage() {
        return cache.getStatistics().getCacheHitPercentage();
    }

    @Override
    public long getCacheMisses() {
        return cache.getStatistics().getCacheMisses();
    }

    @Override
    public float getCacheMissPercentage() {
        return cache.getStatistics().getCacheMissPercentage();
    }

    @Override
    public long getCacheGets() {
        return cache.getStatistics().getCacheGets();
    }

    @Override
    public long getCachePuts() {
        return cache.getStatistics().getCachePuts();
    }

    @Override
    public long getCacheRemovals() {
        return cache.getStatistics().getCacheRemovals();
    }

    @Override
    public long getCacheEvictions() {
        return cache.getStatistics().getCacheEvictions();
    }

    @Override
    public float getAverageGetMillis() {
        return cache.getStatistics().getAverageGetMillis();
    }

    @Override
    public float getAveragePutMillis() {
        return cache.getStatistics().getAveragePutMillis();
    }

    @Override
    public float getAverageRemoveMillis() {
        return cache.getStatistics().getAverageRemoveMillis();
    }

    private String mbeanSafe(String string) {
        return string == null ? "" : string.replaceAll(":|=|\n", ".");
    }
}
