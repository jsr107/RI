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
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract class to help implementers
 * @author Yannis Cosmadopoulos
 */
abstract class AbstractCacheManager implements CacheManager {
    private final HashSet<Class<?>> immutableClasses = new HashSet<Class<?>>();
    private final String name;
    private final ClassLoader classLoader;

    /**
     * Constructor
     * @param name the name
     * @param classLoader the classLoader
     */
    AbstractCacheManager(String name, ClassLoader classLoader) {
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
    public void registerImmutableClass(Class<?> immutableClass) {
        if (immutableClass == null) {
            throw new NullPointerException();
        }
        immutableClasses.add(immutableClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        synchronized (immutableClasses) {
            immutableClasses.clear();
        }
    }

    /**
     * Getter
     * @return the immutable classes
     */
    protected Set<Class<?>> getImmutableClasses() {
        return immutableClasses;
    }

    /**
     * Getter
     * @return the class loader
     */
    protected ClassLoader getClassLoader() {
        return classLoader;
    }
}
