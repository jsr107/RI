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
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResult;
import java.util.List;

/**
 * Details for a method annotated with {@link CacheResult}
 *
 * @author Eric Dalquist
 * @since 1.0
 */
public class CacheResultMethodDetails extends StaticCacheKeyInvocationContext<CacheResult>
    implements StaticCacheResultInvocationContext {

  private final CacheResolver exceptionCacheResolver;

  /**
   * @param cacheMethodDetails
   * @param cacheResolver
   * @param cacheKeyGenerator  The key generator to use
   * @param allParameters      All parameter details
   * @param keyParameters      Parameter details to use for key generation
   */
  public CacheResultMethodDetails(CacheMethodDetails<CacheResult> cacheMethodDetails, CacheResolver cacheResolver,
                                  CacheResolver exceptionCacheResolver, CacheKeyGenerator cacheKeyGenerator,
                                  List<CacheParameterDetails> allParameters,
                                  List<CacheParameterDetails> keyParameters) {

    super(cacheMethodDetails, cacheResolver, cacheKeyGenerator, allParameters, keyParameters);

    this.exceptionCacheResolver = exceptionCacheResolver;
  }

  @Override
  public CacheResolver getExceptionCacheResolver() {
    return this.exceptionCacheResolver;
  }

  /* (non-Javadoc)
   * @see org.jsr107.ri.interceptor.MethodDetails#getInterceptorType()
   */
  @Override
  public InterceptorType getInterceptorType() {
    return InterceptorType.CACHE_RESULT;
  }
}
