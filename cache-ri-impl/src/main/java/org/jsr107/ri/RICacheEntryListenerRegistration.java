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

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;

/**
 * An internal structure to represent the registration of a {@link CacheEntryListener}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @author Brian Oliver
 */
class RICacheEntryListenerRegistration<K, V> {

  private CacheEntryListener<? super K, ? super V> listener;
  private CacheEntryEventFilter<? super K, ? super V> filter;
  private boolean isOldValueRequired;
  private boolean isSynchronous;

  /**
   * Constructs an {@link RICacheEntryListenerRegistration}.
   *
   * @param configuration  the {@link javax.cache.configuration.CacheEntryListenerConfiguration} to be registered
   */
  public RICacheEntryListenerRegistration(CacheEntryListenerConfiguration<K, V> configuration) {
    this.listener = configuration.getCacheEntryListenerFactory().create();
    this.filter = configuration.getCacheEntryEventFilterFactory() == null
                  ? null
                  : configuration.getCacheEntryEventFilterFactory().create();
    this.isOldValueRequired = configuration.isOldValueRequired();
    this.isSynchronous = configuration.isSynchronous();
  }

  /**
   * Obtains the {@link CacheEntryListener} that was registered.
   *
   * @return the {@link CacheEntryListener}
   */
  public CacheEntryListener<? super K, ? super V> getCacheEntryListener() {
    return listener;
  }

  /**
   * Obtains the {@link CacheEntryEventFilter} that was registered.
   *
   * @return the {@link CacheEntryEventFilter}
   */
  public CacheEntryEventFilter<? super K, ? super V> getCacheEntryFilter() {
    return filter;
  }

  /**
   * Determines if the old/previous value should to be supplied with the
   * {@link javax.cache.event.CacheEntryEvent}s dispatched to the
   * {@link CacheEntryListener}.
   */
  public boolean isOldValueRequired() {
    return isOldValueRequired;
  }

  /**
   * Determines if {@link javax.cache.event.CacheEntryEvent}s should be raised
   * synchronously.
   *
   * @return <code>true</code> if events should be raised synchronously
   */
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
    result = prime * result + ((filter == null) ? 0 : filter.hashCode());
    result = prime * result + (isOldValueRequired ? 1231 : 1237);
    result = prime * result + (isSynchronous ? 1231 : 1237);
    result = prime * result
        + ((listener == null) ? 0 : listener.hashCode());
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
    if (!(object instanceof RICacheEntryListenerRegistration)) {
      return false;
    }
    RICacheEntryListenerRegistration<?, ?> other = (RICacheEntryListenerRegistration<?, ?>) object;
    if (filter == null) {
      if (other.filter != null) {
        return false;
      }
    } else if (!filter.equals(other.filter)) {
      return false;
    }
    if (isOldValueRequired != other.isOldValueRequired) {
      return false;
    }
    if (isSynchronous != other.isSynchronous) {
      return false;
    }
    if (listener == null) {
      if (other.listener != null) {
        return false;
      }
    } else if (!listener.equals(other.listener)) {
      return false;
    }
    return true;
  }
}
