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

package org.jsr107.ri.annotations;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.annotation.CacheResult},
 *            {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemoveEntry}, or
 *            {@link javax.cache.annotation.CacheRemoveAll}.
 * @author Eric Dalquist
 * @version $Revision$
 * @since 1.0
 */
public interface StaticCacheInvocationContext<A extends Annotation> extends CacheMethodDetails<A> {

  /**
   * @return The type of intercepter this {@link CacheMethodDetailsImpl} is for
   */
  InterceptorType getInterceptorType();

  /**
   * @return the allParameters in an unmodifiable List
   */
  List<CacheParameterDetails> getAllParameters();

  /**
   * @return The {@link CacheResolver} to use to get the cache for this method
   */
  CacheResolver getCacheResolver();
}
