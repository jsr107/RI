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
package org.jsr107.ri.annotations.cdi;

import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 * Service interface implemented by extensions. An extension is a service provider declared in META-INF/services.
 *
 * @author Greg Luck
 * @since 1.0
 */
public class InterceptorExtension implements Extension {

  /**
   * Service interface implemented by extensions. An extension is a service provider declared in META-INF/services.
   *
   * @param beforeBeanDiscoveryEvent the event to register
   */
  void discoverInterceptorBindings(@Observes BeforeBeanDiscovery beforeBeanDiscoveryEvent) {
    beforeBeanDiscoveryEvent.addInterceptorBinding(CachePut.class);
    beforeBeanDiscoveryEvent.addInterceptorBinding(CacheResult.class);
    beforeBeanDiscoveryEvent.addInterceptorBinding(CacheRemove.class);
    beforeBeanDiscoveryEvent.addInterceptorBinding(CacheRemoveAll.class);
  }

}
