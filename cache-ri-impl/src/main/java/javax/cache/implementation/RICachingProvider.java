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

import javax.cache.OptionalFeature;
import javax.cache.spi.CacheManagerFactory;
import javax.cache.spi.CachingProvider;

/**
 * The reference implementation for JSR107.
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public class RICachingProvider implements CachingProvider {
    @Override
    public CacheManagerFactory getCacheManagerFactory() {
        return RICacheManagerFactory.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * The RI supports {@link OptionalFeature#STORE_BY_REFERENCE}.
     * It does not support {@link OptionalFeature#TRANSACTIONS}
     */
    @Override
    public boolean isSupported(OptionalFeature optionalFeature) {
        switch (optionalFeature) {
            case TRANSACTIONS:
                return false;
            case STORE_BY_REFERENCE:
                return true;
            default:
                return false;
        }
    }

}
