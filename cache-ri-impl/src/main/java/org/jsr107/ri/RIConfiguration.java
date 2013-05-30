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

import javax.cache.CacheLoader;
import javax.cache.CacheWriter;
import javax.cache.Configuration;
import javax.cache.expiry.Eternal;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.Factory;
import javax.cache.event.CacheEntryListenerRegistration;
import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;
import java.util.ArrayList;

/**
 * The reference implementation of a {@link javax.cache.Configuration}.
 *
 * @param <K> the type of keys maintained the cache
 * @param <V> the type of cached values
 * @author Brian Oliver
 * @since 1.0
 */
public class RIConfiguration<K, V> implements Configuration<K, V> {

  /**
   * The expected type of keys (or null if no type-checking required).
   */
  protected Class<K> keyType;

  /**
   * The expected type of values (or null if no type-checking required).
   */
  protected Class<V> valueType;

  /**
   * The {@link CacheEntryListenerRegistration}s for the {@link javax.cache.Configuration}.
   */
  protected ArrayList<CacheEntryListenerRegistration<? super K, ? super V>> cacheEntryListenerRegistrations;

  /**
   * The {@link CacheLoader} {@link Factory} for the {@link javax.cache.Configuration}.
   */
  protected Factory<CacheLoader<K, V>> cacheLoaderFactory;

  /**
   * The {@link CacheWriter} {@link Factory} for the {@link javax.cache.Configuration}.
   */
  protected Factory<CacheWriter<? super K, ? super V>> cacheWriterFactory;

  /**
   * The {@link javax.cache.expiry.ExpiryPolicy} for the {@link javax.cache.Configuration}.
   */
  protected Factory<ExpiryPolicy<? super K, ? super V>> expiryPolicyFactory;

  /**
   * A flag indicating if "read-through" mode is required.
   */
  protected boolean isReadThrough;

  /**
   * A flag indicating if "write-through" mode is required.
   */
  protected boolean isWriteThrough;

  /**
   * A flag indicating if statistics are enabled
   */
  protected boolean isStatisticsEnabled;

  /**
   * A flag indicating if the cache will be store-by-value or store-by-reference.
   */
  protected boolean isStoreByValue;

  /**
   * A flag indicating if the cache will use transactions.
   */
  protected boolean isTransactionsEnabled;

  /**
   * The transaction {@link IsolationLevel}.
   */
  protected IsolationLevel txnIsolationLevel;

  /**
   * The transaction {@link Mode}.
   */
  protected Mode txnMode;

  /**
   * A flag indicating if management is enabled
   */
  protected boolean isManagementEnabled;

  /**
   * Constructs an {@link RIConfiguration} with the standard default values.
   */
  public RIConfiguration() {
    this.keyType = null;
    this.valueType = null;
    this.cacheEntryListenerRegistrations = new ArrayList<CacheEntryListenerRegistration<? super K, ? super V>>();
    this.cacheLoaderFactory = null;
    this.cacheWriterFactory = null;
    this.expiryPolicyFactory = Eternal.<K, V>getFactory();
    this.isReadThrough = false;
    this.isWriteThrough = false;
    this.setStatisticsEnabled(false);
    this.setManagementEnabled(false);
    this.isStoreByValue = true;
    this.isTransactionsEnabled = false;
    this.txnIsolationLevel = IsolationLevel.NONE;
    this.txnMode = Mode.NONE;
  }

  /**
   * A copy-constructor for a {@link RIConfiguration}.
   *
   * @param configuration the {@link javax.cache.Configuration} from which to copy
   */
  public RIConfiguration(Configuration<K, V> configuration) {

    this.keyType = configuration.getKeyType();
    this.valueType = configuration.getValueType();

    this.cacheEntryListenerRegistrations = new ArrayList<CacheEntryListenerRegistration<? super K, ? super V>>();
    for (CacheEntryListenerRegistration<? super K, ? super V> r : configuration.getCacheEntryListenerRegistrations()) {
      RICacheEntryListenerRegistration<? super K, ? super V> registration =
          new RICacheEntryListenerRegistration<K, V>(r.getCacheEntryListener(),
              r.getCacheEntryFilter(),
              r.isOldValueRequired(),
              r.isSynchronous());
      this.cacheEntryListenerRegistrations.add(registration);
    }

    this.cacheLoaderFactory = configuration.getCacheLoaderFactory();
    this.cacheWriterFactory = configuration.getCacheWriterFactory();

    if (configuration.getExpiryPolicyFactory() == null) {
      this.expiryPolicyFactory = Eternal.<K, V>getFactory();
    } else {
      this.expiryPolicyFactory = configuration.getExpiryPolicyFactory();
    }

    this.isReadThrough = configuration.isReadThrough();
    this.isWriteThrough = configuration.isWriteThrough();

    this.setStatisticsEnabled(configuration.isStatisticsEnabled());
    this.setManagementEnabled(configuration.isManagementEnabled());

    this.isStoreByValue = configuration.isStoreByValue();

    this.isTransactionsEnabled = configuration.isTransactionsEnabled();
    this.txnIsolationLevel = configuration.getTransactionIsolationLevel();
    this.txnMode = configuration.getTransactionMode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<K> getKeyType() {
    return keyType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<V> getValueType() {
    return valueType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<CacheEntryListenerRegistration<? super K, ? super V>> getCacheEntryListenerRegistrations() {
    return cacheEntryListenerRegistrations;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Factory<CacheLoader<K, V>> getCacheLoaderFactory() {
    return this.cacheLoaderFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Factory<CacheWriter<? super K, ? super V>> getCacheWriterFactory() {
    return this.cacheWriterFactory;
  }

  /**
   * {@inheritDoc}
   */
  public Factory<ExpiryPolicy<? super K, ? super V>> getExpiryPolicyFactory() {
    return this.expiryPolicyFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IsolationLevel getTransactionIsolationLevel() {
    return this.txnIsolationLevel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mode getTransactionMode() {
    return this.txnMode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReadThrough() {
    return this.isReadThrough;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isWriteThrough() {
    return this.isWriteThrough;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isStoreByValue() {
    return this.isStoreByValue;
  }

  /**
   * A flag indicating if statistics gathering is enabled.
   * {@inheritDoc}
   */
  @Override
  public boolean isStatisticsEnabled() {
    return this.isStatisticsEnabled;
  }


  /**
   * Whether management is enabled for this cache
   * {@inheritDoc}
   */
  @Override
  public boolean isManagementEnabled() {
    return this.isManagementEnabled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isTransactionsEnabled() {
    return this.isTransactionsEnabled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result +
        ((keyType == null) ? 0 : keyType.hashCode());
    result = prime * result +
        ((valueType == null) ? 0 : valueType.hashCode());
    result = prime
        * result
        + ((cacheEntryListenerRegistrations == null) ? 0 : cacheEntryListenerRegistrations
        .hashCode());
    result = prime * result
        + ((cacheLoaderFactory == null) ? 0 : cacheLoaderFactory.hashCode());
    result = prime * result
        + ((cacheWriterFactory == null) ? 0 : cacheWriterFactory.hashCode());
    result = prime * result
        + ((expiryPolicyFactory == null) ? 0 : expiryPolicyFactory.hashCode());
    result = prime * result + (isReadThrough ? 1231 : 1237);
    result = prime * result + (isStatisticsEnabled() ? 1231 : 1237);
    result = prime * result + (isStoreByValue ? 1231 : 1237);
    result = prime * result + (isWriteThrough ? 1231 : 1237);
    result = prime
        * result
        + ((txnIsolationLevel == null) ? 0 : txnIsolationLevel
        .hashCode());
    result = prime * result + ((txnMode == null) ? 0 : txnMode.hashCode());
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
    if (!(object instanceof RIConfiguration)) {
      return false;
    }
    RIConfiguration<?, ?> other = (RIConfiguration<?, ?>) object;
    if (keyType == null) {
      if (other.keyType != null) {
        return false;
      }
    } else if (!keyType.equals(other.keyType)) {
      return false;
    }
    if (valueType == null) {
      if (other.valueType != null) {
        return false;
      }
    } else if (!valueType.equals(other.valueType)) {
      return false;
    }
    if (cacheEntryListenerRegistrations == null) {
      if (other.cacheEntryListenerRegistrations != null) {
        return false;
      }
    } else if (!cacheEntryListenerRegistrations.equals(other.cacheEntryListenerRegistrations)) {
      return false;
    }
    if (cacheLoaderFactory == null) {
      if (other.cacheLoaderFactory != null) {
        return false;
      }
    } else if (!cacheLoaderFactory.equals(other.cacheLoaderFactory)) {
      return false;
    }
    if (cacheWriterFactory == null) {
      if (other.cacheWriterFactory != null) {
        return false;
      }
    } else if (!cacheWriterFactory.equals(other.cacheWriterFactory)) {
      return false;
    }
    if (expiryPolicyFactory == null) {
      if (other.expiryPolicyFactory != null) {
        return false;
      }
    } else if (!expiryPolicyFactory.equals(other.expiryPolicyFactory)) {
      return false;
    }
    if (isReadThrough != other.isReadThrough) {
      return false;
    }
    if (isStatisticsEnabled() != other.isStatisticsEnabled()) {
      return false;
    }
    if (isManagementEnabled() != other.isManagementEnabled()) {
      return false;
    }
    if (isStoreByValue != other.isStoreByValue) {
      return false;
    }
    if (isWriteThrough != other.isWriteThrough) {
      return false;
    }
    if (isTransactionsEnabled != other.isTransactionsEnabled) {
      return false;
    }
    if (txnIsolationLevel != other.txnIsolationLevel) {
      return false;
    }
    if (txnMode != other.txnMode) {
      return false;
    }
    return true;
  }


  /**
   * Sets whether statistics gathering is enabled on a cache.
   * <p/>
   *
   * @param enabled true to enable statistics, false to disable.
   */
  public void setStatisticsEnabled(boolean enabled) {
    isStatisticsEnabled = enabled;
  }

  /**
   * Sets whether management is enabled on a cache.
   * <p/>
   *
   * @param enabled true to enable, false to disable.
   */
  public void setManagementEnabled(boolean enabled) {
    isManagementEnabled = enabled;
  }
}
