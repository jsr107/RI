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

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheResolver;
import java.util.List;

/**
 * Details for a method annotated with {@link CachePut}
 *
 * @author Eric Dalquist
 * @since 1.0
 */
public class CachePutMethodDetails extends StaticCacheKeyInvocationContext<CachePut> {
  private final CacheParameterDetails cacheValueParameter;

  /**
   * @param cacheMethodDetails
   * @param cacheResolver
   * @param cacheKeyGenerator   The key generator to use
   * @param allParameters       All parameter details
   * @param keyParameters       Parameter details to use for key generation
   * @param cacheValueParameter The parameter to store in the cache
   */
  public CachePutMethodDetails(CacheMethodDetails<CachePut> cacheMethodDetails, CacheResolver cacheResolver,
                               CacheKeyGenerator cacheKeyGenerator, List<CacheParameterDetails> allParameters,
                               List<CacheParameterDetails> keyParameters, CacheParameterDetails cacheValueParameter) {

    super(cacheMethodDetails, cacheResolver, cacheKeyGenerator, allParameters, keyParameters);

    if (cacheValueParameter == null) {
      throw new IllegalArgumentException("cacheValueParameter cannot be null");
    }

    this.cacheValueParameter = cacheValueParameter;
  }


  /* (non-Javadoc)
   * @see org.jsr107.ri.interceptor.MethodDetails#getInterceptorType()
   */
  @Override
  public InterceptorType getInterceptorType() {
    return InterceptorType.CACHE_PUT;
  }

  /**
   * @return the cacheValueParameter
   */
  public CacheParameterDetails getCacheValueParameter() {
    return this.cacheValueParameter;
  }
}
