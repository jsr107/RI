/**
 *  Copyright 2012 Terracotta, Inc.
 *  Copyright 2012 Oracle, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.cache.CacheConfiguration;
import javax.cache.CacheConfiguration.Duration;

import org.junit.Test;

/**
 * Unit tests for a {@link CacheConfiguration}.
 *
 * @author Brian Oliver
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public class RICacheConfigurationTest {
    
    public <K, V> CacheConfiguration<K, V> getCacheConfiguration()
    {
        return new RICacheConfiguration<K, V>();
    }
    
    @Test
    public void checkDefaults() {
        CacheConfiguration<?, ?> config = getCacheConfiguration();
        assertFalse(config.isReadThrough());
        assertFalse(config.isWriteThrough());
        assertFalse(config.isStatisticsEnabled());
        assertTrue(config.isStoreByValue());
        
        Duration duration = new Duration(TimeUnit.MINUTES, 10);
        assertEquals(Duration.ETERNAL, config.getCacheEntryExpiryPolicy().getTTLForCreatedEntry(null));
        assertEquals(duration, config.getCacheEntryExpiryPolicy().getTTLForAccessedEntry(null, duration));
        assertEquals(duration, config.getCacheEntryExpiryPolicy().getTTLForModifiedEntry(null, duration));
    }

    @Test
    public void notSame() {
        CacheConfiguration<?, ?> config1 = getCacheConfiguration();
        CacheConfiguration<?, ?> config2 = getCacheConfiguration();
        assertNotSame(config1, config2);
    }

    @Test
    public void equals() {
        CacheConfiguration<?, ?> config1 = getCacheConfiguration();
        CacheConfiguration<?, ?> config2 = getCacheConfiguration();
        assertEquals(config1, config2);
    }

    @Test
    public void equalsNotEquals() {
        CacheConfiguration<?, ?> config1 = getCacheConfiguration();
        config1.setStatisticsEnabled(!config1.isStatisticsEnabled());
        
        CacheConfiguration<?, ?> config2 = getCacheConfiguration();
        assertFalse(config1.equals(config2));
    }

    @Test
    public void setStatisticsEnabled() {
        CacheConfiguration<?, ?> config = getCacheConfiguration();
        boolean isStatisticsEnabled = config.isStatisticsEnabled();
        config.setStatisticsEnabled(!isStatisticsEnabled);
        assertEquals(!isStatisticsEnabled, config.isStatisticsEnabled());
    }

    @Test
    public void DurationEquals() {
        CacheConfiguration.Duration duration1 = new CacheConfiguration.Duration(TimeUnit.DAYS, 2);
        CacheConfiguration.Duration duration2 = new CacheConfiguration.Duration(TimeUnit.DAYS, 2);
        assertEquals(duration1, duration2);
    }


    @Test
    public void durationNotEqualsAmount() {
        CacheConfiguration.Duration duration1 = new CacheConfiguration.Duration(TimeUnit.DAYS, 2);
        CacheConfiguration.Duration duration2 = new CacheConfiguration.Duration(TimeUnit.DAYS, 3);
        assertFalse(duration1.equals(duration2));
        assertFalse(duration1.hashCode() == duration2.hashCode());
    }

    @Test
    public void durationNotEqualsUnit() {
        CacheConfiguration.Duration duration1 = new CacheConfiguration.Duration(TimeUnit.DAYS, 2);
        CacheConfiguration.Duration duration2 = new CacheConfiguration.Duration(TimeUnit.MINUTES, 2);
        assertFalse(duration1.equals(duration2));
        assertFalse(duration1.hashCode() == duration2.hashCode());

    }

    /**
     * Checks that equals() is semantically meaningful.
     *
     * Also verifies the second requirement in the contract of hashcode:
     * * <li>If two objects are equal according to the <tt>equals(Object)</tt>
     *     method, then calling the <code>hashCode</code> method on each of
     *     the two objects must produce the same integer result.
     */
    @Test
    public void durationEqualsWhenSemanticallyEqualsButExpressedDifferentUnits() {
        CacheConfiguration.Duration duration1 = new CacheConfiguration.Duration(TimeUnit.SECONDS, 120);
        CacheConfiguration.Duration duration2 = new CacheConfiguration.Duration(TimeUnit.MINUTES, 2);
        assertEquals(duration1, duration2);
        assertEquals(duration1.hashCode(), duration2.hashCode());
    }

    @Test
    public void durationEqualsWhenSemanticallyEqualsButExpressedDifferentUnitsHashCode() {
        CacheConfiguration.Duration duration1 = new CacheConfiguration.Duration(TimeUnit.SECONDS, 120);
        CacheConfiguration.Duration duration2 = new CacheConfiguration.Duration(TimeUnit.MINUTES, 2);
        assertEquals(duration1, duration2);
        assertEquals(duration1.hashCode(), duration2.hashCode());
    }


    @Test
    public void durationNotEqualsUnitEquals() {
        long time = 2;
        CacheConfiguration.Duration duration1 = new CacheConfiguration.Duration(TimeUnit.HOURS, 2);
        time *= 60;
        CacheConfiguration.Duration duration2 = new CacheConfiguration.Duration(TimeUnit.MINUTES, 120);
        assertEquals(duration1, duration2);
        time *= 60;
        duration2 = new CacheConfiguration.Duration(TimeUnit.SECONDS, time);
        assertEquals(duration1, duration2);
        time *= 1000;
        duration2 = new CacheConfiguration.Duration(TimeUnit.MILLISECONDS, time);
        assertEquals(duration1, duration2);
    }


    @Test
    public void DurationExceptions() {
        try {
            new CacheConfiguration.Duration(null, 2);
        } catch (NullPointerException e) {
            //expected
        }

        try {
            new CacheConfiguration.Duration(TimeUnit.MINUTES, 0);
        } catch (NullPointerException e) {
            //expected
        }


        try {
            new CacheConfiguration.Duration(TimeUnit.MICROSECONDS, 10);
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            new CacheConfiguration.Duration(TimeUnit.MILLISECONDS, -10);
        } catch (IllegalArgumentException e) {
            //expected
        }
    }
}
