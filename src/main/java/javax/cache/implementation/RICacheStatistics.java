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
import javax.cache.CacheStatisticsMBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;


/**
 * The reference implementation for JSR107.
 * <p/>
 */
public class RICacheStatistics implements CacheStatisticsMBean, Serializable {

    private transient Cache cache;

    private final ObjectName objectName;

    private final AtomicLong cacheRemovals = new AtomicLong();
    private final AtomicLong cacheExpiries = new AtomicLong();
    private final AtomicLong cachePuts = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong cacheEvictions = new AtomicLong();


    /**
     * Constructs a cache statistics object
     *
     * @param cache the associated cache
     * @param cacheManagerName the name of the cache manager
     */
    public RICacheStatistics(Cache cache, String cacheManagerName) {
        this.cache = cache;
        objectName = createObjectName(cacheManagerName, cache.getCacheName());
    }

    /**
     * Creates an object name using the scheme "javax.cache:type=CacheStatistics,CacheManager=<cacheManagerName>,name=<cacheName>"
     */
    static ObjectName createObjectName(String cacheManagerName, String cacheName) {
        ObjectName objectName;
        try {
            objectName = new ObjectName("javax.cache:type=CacheStatistics,CacheManager="
                    + cacheManagerName + ",name=" + mbeanSafe(cacheName));
        } catch (MalformedObjectNameException e) {
            throw new CacheException(e);
        }
        return objectName;
    }

    /**
     * Filter out invalid ObjectName characters from string.
     *
     * @param string input string
     * @return A valid JMX ObjectName attribute value.
     */
    public static String mbeanSafe(String string) {
        return string == null ? "" : string.replaceAll(":|=|\n", ".");
    }


    /**
     * {@inheritDoc}
     */
    public String getName() {
        return cache.getCacheName();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        return cache.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        cachePuts.set(0);
        cacheMisses.set(0);
        cacheRemovals.set(0);
        cacheExpiries.set(0);
        cacheHits.set(0);
        cacheEvictions.set(0);
    }

    /**
     * {@inheritDoc}
     */
    public long getEntryCount() {
        return ((RICache) cache).getSize();
    }

    /**
     * @return the number of hits
     */
    public long getCacheHits() {
        return cacheHits.longValue();
    }

    /**
     * Returns cache hits as a percentage of total gets.
     *
     * @return the percentage of successful hits, as a decimal
     */
    public float getCacheHitPercentage() {
        return getCacheHits() / getCacheGets();
    }

    /**
     * @return the number of misses
     */
    public long getCacheMisses() {
        return cacheMisses.longValue();
    }

    /**
     * Returns cache misses as a percentage of total gets.
     *
     * @return the percentage of accesses that failed to find anything
     */
    public float getCacheMissPercentage() {
        return getCacheMisses() / getCacheGets();
    }

    /**
     * The total number of requests to the cache. This will be equal to the sum of the hits and misses.
     * <p/>
     * A "get" is an operation that returns the current or previous value.
     *
     * @return the number of hits
     */
    public long getCacheGets() {
        return getCacheHits() + getCacheMisses();
    }

    /**
     * The total number of puts to the cache.
     * <p/>
     * A put is counted even if it is immediately evicted. A replace invcludes a put and remove.
     *
     * @return the number of hits
     */
    public long getCachePuts() {
        return cachePuts.longValue();
    }

    /**
     * The total number of removals from the cache. This does not include evictions, where the cache itself
     * initiates the removal to make space.
     * <p/>
     * A replace invcludes a put and remove.
     *
     * @return the number of hits
     */
    public long getCacheRemovals() {
        return cacheRemovals.longValue();
    }

    /**
     * @return the number of evictions from the cache
     */
    public long getCacheEvictions() {
        return cacheEvictions.longValue();
    }

    //package local incrementers

    /**
     * Increases the counter by the number specified.
     * @param number the number to increase the counter by
     */
    void increaseCacheRemovals(long number) {
        cacheRemovals.getAndAdd(number);
    }

    /**
     * Increases the counter by the number specified.
     * @param number the number to increase the counter by
     */
    void increaseCacheExpiries(long number) {
        cacheExpiries.getAndAdd(number);
    }

    /**
     * Increases the counter by the number specified.
     * @param number the number to increase the counter by
     */
    void increaseCachePuts(long number) {
        cachePuts.getAndAdd(number);
    }

    /**
     * Increases the counter by the number specified.
     * @param number the number to increase the counter by
     */
    void increaseCacheHits(long number) {
        cacheHits.getAndAdd(number);
    }

    /**
     * Increases the counter by the number specified.
     * @param number the number to increase the counter by
     */
    void increaseCacheMisses(long number) {
        cacheMisses.getAndAdd(number);
    }

    /**
     * Increases the counter by the number specified.
     * @param number the number to increase the counter by
     */
    void increaseCacheEvictions(long number) {
        cacheEvictions.getAndAdd(number);
    }

}
