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

import javax.cache.CacheException;
import javax.cache.CacheMXBean;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;


/**
 * A convenience class for registering CacheStatisticsMBeans with an MBeanServer.
 *
 * @author Greg Luck
 * @since 1.0
 */
public final class MBeanServerRegistrationUtility {


    private MBeanServerRegistrationUtility() {
        //prevent construction
    }


    /**
     * Utility method for registering CacheStatistics with the platform MBeanServer
     * @param cache the cache to register
     */
    static void registerCacheStatistics(RICache cache) {
        CacheMXBean mBean =  cache.getMBean();
        if (mBean != null) {
            //these can change during runtime, so always look it up
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName registeredObjectName = calculateCacheStatisticsObjectName(cache.getCacheManager().getName(), mBean.getName());
            try {
                mBeanServer.registerMBean(mBean, registeredObjectName);
            } catch (Exception e) {
                throw new CacheException("Error registering cache MXBeans for CacheManager "
                        + registeredObjectName + " . Error was " + e.getMessage(), e);
            }
        }
    }



    /**
     * Remove MXBeans on shutdown.
     * todo should be done cache by cache
     * Removes registered ObjectNames
     *
     * @throws CacheException - all exceptions are wrapped in CacheException
     */
    static void unregisterAllCaches(String cacheManagerName) {

        Set<ObjectName> registeredObjectNames = null;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            registeredObjectNames = mBeanServer.queryNames(
                    new ObjectName("javax.cache:*,CacheManager=" + cacheManagerName), null);
        } catch (MalformedObjectNameException e) {
            // this should not happen
            throw new CacheException("Error querying MBeanServer. Error was " + e.getMessage(), e);
        }
        for (ObjectName registeredObjectName : registeredObjectNames) {
            try {
                mBeanServer.unregisterMBean(registeredObjectName);
            } catch (Exception e) {
                throw new CacheException("Error unregistering object instance "
                        + registeredObjectName + " . Error was " + e.getMessage(), e);
            }
        }
    }

    /**
     * Creates an object name using the scheme "javax.cache:type=CacheStatistics,CacheManager=<cacheManagerName>,name=<cacheName>"
     * todo work out name scheme once examined in JConsole
     */
    private static ObjectName calculateCacheStatisticsObjectName(String cacheManagerName, String cacheName) {
        try {
            return new ObjectName("javax.cache:type=CacheStatistics,CacheManager="
                    + cacheManagerName + ",name=" + mbeanSafe(cacheName));
        } catch (MalformedObjectNameException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Creates an object name using the scheme "javax.cache:type=CacheStatistics,CacheManager=<cacheManagerName>,name=<cacheName>"
     */
    private static ObjectName calculateCacheObjectName(String cacheManagerName, String cacheName) {
        try {
            return new ObjectName("javax.cache:type=Cache,CacheManager="
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
    private static String mbeanSafe(String string) {
        return string == null ? "" : string.replaceAll(":|=|\n", ".");
    }




}

