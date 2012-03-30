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

import javax.cache.CacheManager;

/**
 * Abstract class to help implementers
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public abstract class AbstractCacheManager implements CacheManager {
    private final String name;
    private final ClassLoader classLoader;

    /**
     * Constructor
     * @param name the name
     * @param classLoader the classLoader
     */
    public AbstractCacheManager(String name, ClassLoader classLoader) {
        this.name = name;
        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
    }

    /**
     * Getter
     * @return the class loader
     */
    protected ClassLoader getClassLoader() {
        return classLoader;
    }
}
