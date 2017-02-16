/**
 *  Copyright 2011-2013 Terracotta, Inc.
 *  Copyright 2011-2013 Oracle America Incorporated
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
package org.jsr107.ri.annotations.cdi;

import javax.cache.annotation.CacheResolverFactory;
import javax.enterprise.inject.Produces;

import org.jsr107.ri.annotations.DefaultCacheResolverFactory;

/**
 * Producer for the default {@link javax.cache.annotation.CacheResolverFactory}.
 *
 * @author Sven Haberer
 * @since 1.0.0-injectability-improvement
 */
public class CacheResolverFactoryProducer {
  /**
   * Produces a new instance of the {@link org.jsr107.ri.annotations.DefaultCacheResolverFactory}.
   *
   * @return A new instance of the {@link org.jsr107.ri.annotations.DefaultCacheResolverFactory}.
   */
  @Produces
  @UsedByDefault
  public CacheResolverFactory produce() {
    return new DefaultCacheResolverFactory();
  }
}
