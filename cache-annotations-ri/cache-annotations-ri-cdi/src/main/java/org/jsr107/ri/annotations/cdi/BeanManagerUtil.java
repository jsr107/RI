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

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * @author Rick Hightower
 * @since 1.0
 */
public class BeanManagerUtil {


  @Inject
  private BeanManager beanManager;

  /**
   * @return the bean
   */
  public <T> T getBeanByType(Class<T> type, Annotation... qualifiers) {
    if (type == null) {
      throw new IllegalArgumentException("CDI Bean type cannot be null");
    }

    Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
    if (beans.isEmpty()) {
      return null;
    }
    Bean<?> bean = beanManager.resolve(beans);
    CreationalContext<?> context = beanManager
        .createCreationalContext(bean);
    @SuppressWarnings("unchecked")
    T result = (T) beanManager.getReference(bean, bean.getBeanClass(),
        context);
    return result;
  }


}
