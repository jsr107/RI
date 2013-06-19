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

import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerFactoryDefinition;

/**
 * The reference implementation of the {@link javax.cache.event.CacheEntryListenerFactoryDefinition}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @author Brian Oliver
 */
public class RICacheEntryListenerFactoryDefinition<K, V> implements CacheEntryListenerFactoryDefinition<K, V> {

  private Factory<CacheEntryListener<? super K, ? super V>> listenerFactory;
  private Factory<CacheEntryEventFilter<? super K, ? super V>> filterFactory;
  private boolean isOldValueRequired;
  private boolean isSynchronous;

  /**
   * Constructs an {@link RICacheEntryListenerFactoryDefinition}.
   *
   * @param listenerFactory    the {@link CacheEntryListener} {@link Factory}
   * @param filterFactory      the optional {@link CacheEntryEventFilter} {@link Factory}
   * @param isOldValueRequired if the old value is required for events with this listenerFactory
   * @param isSynchronous      if the listenerFactory should block the thread causing the event
   */
  public RICacheEntryListenerFactoryDefinition(Factory<CacheEntryListener<? super K, ? super V>> listenerFactory,
                                               Factory<CacheEntryEventFilter<? super K, ? super V>> filterFactory,
                                               boolean isOldValueRequired,
                                               boolean isSynchronous) {
    this.listenerFactory = listenerFactory;
    this.filterFactory = filterFactory;
    this.isOldValueRequired = isOldValueRequired;
    this.isSynchronous = isSynchronous;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Factory<CacheEntryEventFilter<? super K, ? super V>> getCacheEntryFilterFactory() {
    return filterFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Factory<CacheEntryListener<? super K, ? super V>> getCacheEntryListenerFactory() {
    return listenerFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isOldValueRequired() {
    return isOldValueRequired;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSynchronous() {
    return isSynchronous;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((filterFactory == null) ? 0 : filterFactory.hashCode());
    result = prime * result + (isOldValueRequired ? 1231 : 1237);
    result = prime * result + (isSynchronous ? 1231 : 1237);
    result = prime * result
        + ((listenerFactory == null) ? 0 : listenerFactory.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (!(object instanceof RICacheEntryListenerFactoryDefinition)) {
      return false;
    }
    RICacheEntryListenerFactoryDefinition<?, ?> other = (RICacheEntryListenerFactoryDefinition<?, ?>) object;
    if (filterFactory == null) {
      if (other.filterFactory != null) {
        return false;
      }
    } else if (!filterFactory.equals(other.filterFactory)) {
      return false;
    }
    if (isOldValueRequired != other.isOldValueRequired) {
      return false;
    }
    if (isSynchronous != other.isSynchronous) {
      return false;
    }
    if (listenerFactory == null) {
      if (other.listenerFactory != null) {
        return false;
      }
    } else if (!listenerFactory.equals(other.listenerFactory)) {
      return false;
    }
    return true;
  }
}
