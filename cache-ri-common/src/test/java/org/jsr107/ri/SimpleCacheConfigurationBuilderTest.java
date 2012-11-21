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

import org.jsr107.ri.SimpleCacheConfigurationBuilder;
import org.junit.Test;

import javax.cache.CacheConfiguration;
import javax.cache.CacheLoader;
import javax.cache.CacheWriter;
import javax.cache.Caching;
import javax.cache.InvalidConfigurationException;
import javax.cache.OptionalFeature;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link CacheConfigurationBuilder}s.
 *
 * @author Yannis Cosmadopoulos
 * @author Brian Oliver
 * 
 * @since 1.0
 */
public class SimpleCacheConfigurationBuilderTest {

    public <K, V> CacheConfigurationBuilder<K, V, ?> getCacheConfigurationBuilder() {
    	return new SimpleCacheConfigurationBuilder<K, V>();
    }
    
    public void setCacheLoader_Null() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        CacheLoader<Integer, String> cl = null;
        builder.setCacheLoader(cl);
        CacheConfiguration<Integer, String> config = builder.build();
        assertEquals(null, config.getCacheLoader());
    }

    @Test(expected=InvalidConfigurationException.class)
    public void setReadThrough_NoCacheLoader() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        builder.setReadThrough(true);
        builder.build(); // cache loader is null
        fail();
    }

    public void setCacheWriter_Null() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        CacheWriter<Integer, String> cw = null;
        builder.setCacheWriter(cw);
        CacheConfiguration<Integer, String> config = builder.build();
        assertEquals(null, config.getCacheWriter());
    }

    @Test(expected=InvalidConfigurationException.class)
    public void setWriteThrough_NoCacheWriter() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        builder.setWriteThrough(true);
        
        builder.build(); // cache writer is null
        fail();
    }

    @Test
    public void setTransactionEnabled() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        IsolationLevel isolationLevel = null;
        Mode mode = null;
        CacheConfiguration<Integer, String> config = builder.setTransactions(isolationLevel, mode).build();
        assertTrue(config.isTransactionEnabled());
    }

    @Test
    public void setStoreByValue_false() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
    	CacheConfiguration<Integer, String> config = builder.setStoreByValue(false).build();
        assertFalse(config.isStoreByValue());
    }

    @Test(expected=NullPointerException.class)
    public void setExpiry_null() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        builder.setExpiry(null, CacheConfiguration.Duration.ETERNAL);
        fail();
    }

    @Test
    public void setExpiry_good_null() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        try {
            builder.setExpiry(CacheConfiguration.ExpiryType.MODIFIED, null);
            fail();
        } catch(NullPointerException e) {
            //
        }
    }

    @Test
    public void getExpiry_default() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        CacheConfiguration<Integer, String> config = builder.build();
        assertEquals(CacheConfiguration.ExpiryType.MODIFIED, config.getExpiryType());
        assertEquals(CacheConfiguration.Duration.ETERNAL, config.getExpiryDuration());
    }

    @Test
    public void setExpiry_accessed() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        CacheConfiguration.ExpiryType type = CacheConfiguration.ExpiryType.ACCESSED;
        CacheConfiguration.Duration duration = new CacheConfiguration.Duration(TimeUnit.MINUTES, 4L);
        builder.setExpiry(type, duration);
        CacheConfiguration<Integer, String> config = builder.build();
        assertEquals(duration, config.getExpiryDuration());
    }

    @Test
    public void setExpiry_modified() {
        CacheConfigurationBuilder<Integer, String, ?> builder = getCacheConfigurationBuilder();
        CacheConfiguration.ExpiryType type = CacheConfiguration.ExpiryType.MODIFIED;
        CacheConfiguration.Duration duration = new CacheConfiguration.Duration(TimeUnit.HOURS, 4L);
        builder.setExpiry(type, duration);
        CacheConfiguration<Integer, String> config = builder.build();
        assertEquals(duration, config.getExpiryDuration());
    }

    protected boolean isSupported(OptionalFeature feature) {
        return Caching.isSupported(feature);
    }
}
