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
import javax.cache.OptionalFeature;

/**
 * The reference implementation for JSR107.
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 */
public class RICachingProvider extends AbstractCachingProvider {
    /**
     * {@inheritDoc}
     */
    @Override
    public CacheManager createCacheManager(ClassLoader classLoader, String name) {
        if (name == null) {
            throw new NullPointerException("CacheManager name not specified");
        }
        return new RICacheManager(name, classLoader);
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
