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

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * @author Eric Dalquist
 * @since 1.0
 */
public class CacheParameterDetails {
  private final Class<?> rawType;
  private final Set<Annotation> annotations;
  private final int parameterPosition;

  /**
   * Create new cache parameter details
   *
   * @param rawType           The declared class of the parameter
   * @param annotations       All annotations on the parameter
   * @param parameterPosition The position of the parameter in the original parameter array
   */
  public CacheParameterDetails(Class<?> rawType, Set<Annotation> annotations, int parameterPosition) {
    this.rawType = rawType;
    this.annotations = annotations;
    this.parameterPosition = parameterPosition;
  }

  /**
   * @see javax.cache.annotation.CacheInvocationParameter#getRawType()
   */
  public Class<?> getRawType() {
    return this.rawType;
  }

  /**
   * @see javax.cache.annotation.CacheInvocationParameter#getAnnotations()
   */
  public Set<Annotation> getAnnotations() {
    return this.annotations;
  }

  /**
   * @see javax.cache.annotation.CacheInvocationParameter#getParameterPosition()
   */
  public int getParameterPosition() {
    return this.parameterPosition;
  }
}
