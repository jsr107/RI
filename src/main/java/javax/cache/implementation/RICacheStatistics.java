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
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;


/**
 * The reference implementation for JSR107.
 * <p/>
 */
public class RICacheStatistics implements CacheStatisticsMBean, Serializable {

    private transient Cache cache;

    //TODO greg: use this, finish off
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
        objectName = createObjectName(cacheManagerName, cache.getName());
    }

    /**
     * Creates an object name using the scheme "javax.cache:type=CacheStatistics,CacheManager=<cacheManagerName>,name=<cacheName>"
     */
    private ObjectName createObjectName(String cacheManagerName, String cacheName) {
        try {
            return new ObjectName("javax.cache:type=CacheStatistics,CacheManager="
                    + cacheManagerName + ",name=" + mbeanSafe(cacheName));
        } catch (MalformedObjectNameException e) {
            throw new CacheException(e);
        }
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
    @Override
    public String getName() {
        return cache.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatus() {
        return cache.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearStatistics() {
        cachePuts.set(0);
        cacheMisses.set(0);
        cacheRemovals.set(0);
        cacheExpiries.set(0);
        cacheHits.set(0);
        cacheEvictions.set(0);
    }

    /**
     * The date from which statistics have been accumulated. Because statistics can be cleared, this is not necessarily
     * since the cache was started.
     *
     * @return the date statistics started being accumulated
     */
    @Override
    public Date statsAccumulatingFrom() {
        return null;
        //Todo change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * @return the entry count
     */
    public long getEntryCount() {
        return ((RICache) cache).getSize();
    }

    /**
     * @return the number of hits
     */
    @Override
    public long getCacheHits() {
        return cacheHits.longValue();
    }

    /**
     * Returns cache hits as a percentage of total gets.
     *
     * @return the percentage of successful hits, as a decimal
     */
    @Override
    public float getCacheHitPercentage() {
        return getCacheHits() / getCacheGets();
    }

    /**
     * @return the number of misses
     */
    @Override
    public long getCacheMisses() {
        return cacheMisses.longValue();
    }

    /**
     * Returns cache misses as a percentage of total gets.
     *
     * @return the percentage of accesses that failed to find anything
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
    public long getCacheRemovals() {
        return cacheRemovals.longValue();
    }

    /**
     * @return the number of evictions from the cache
     */
    @Override
    public long getCacheEvictions() {
        return cacheEvictions.longValue();
    }

    /**
     * The mean time to execute gets.
     *
     * @return the time in milliseconds
     */
    @Override
    public long getAverageGetMillis() {
        return 0;
        //Todo change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * The mean time to execute puts.
     *
     * @return the time in milliseconds
     */
    @Override
    public long getAveragePutMillis() {
        return 0;
        //Todo change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * The mean time to execute removes.
     *
     * @return the time in milliseconds
     */
    @Override
    public long getAverageRemoveMillis() {
        //Todo change body of implemented methods use File | Settings | File Templates.
        return 0;
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
