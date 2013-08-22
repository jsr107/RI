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

package org.jsr107.ri.processor;

import org.jsr107.ri.RICacheEventDispatcher;
import org.jsr107.ri.RICachedValue;
import org.jsr107.ri.RIInternalConverter;

import javax.cache.processor.MutableEntry;

/**
 * A {@link javax.cache.processor.MutableEntry} that is used by {@link javax.cache.processor.EntryProcessor}s.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values*
 * @author Greg Luck
 */
public class EntryProcessorEntry<K, V> implements MutableEntry<K, V> {
  /**
   * The key of the {@link MutableEntry}.
   */
  private final K key;

  /**
   * The {@link org.jsr107.ri.RICachedValue} for the {@link MutableEntry}.
   */
  private final RICachedValue cachedValue;

  /**
   * The RICache this entry belongs to.
   */
  private final RIInternalConverter<V> converter;

  /**
   * The new value for the {@link MutableEntry}.
   */
  private V value;

  /**
   * The {@link MutableEntryOperation} to be performed on the {@link MutableEntry}.
   */
  private MutableEntryOperation operation;

  /**
   * The time (since the Epoc) when the MutableEntry was created.
   */
  private long now;

  /**
   * The dispatcher to use for capturing events to eventually dispatch.
   */
  private RICacheEventDispatcher<K, V> dispatcher;

  /**
   * Construct a {@link MutableEntry}
   *
   * @param key         the key for the {@link MutableEntry}
   * @param cachedValue the {@link RICachedValue} of the {@link MutableEntry}
   *                    (may be <code>null</code>)
   * @param now         the current time
   * @param dispatcher  the dispatch to capture events to dispatch
   */
  public EntryProcessorEntry(RIInternalConverter<V> converter, K key,
                      RICachedValue cachedValue, long now,
                      RICacheEventDispatcher<K, V> dispatcher) {
    this.converter = converter;
    this.key = key;
    this.cachedValue = cachedValue;
    this.operation = MutableEntryOperation.NONE;
    this.value = null;
    this.now = now;
    this.dispatcher = dispatcher;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public K getKey() {
    return key;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public V getValue() {
    if (operation == MutableEntryOperation.NONE) {
      if (cachedValue == null || cachedValue.isExpiredAt(now)) {
        value = null;
      } else if (value == null) {
        Object internalValue = cachedValue.getInternalValue(now);
        value = internalValue == null ? null : converter.fromInternal(internalValue);
      }
    }

    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean exists() {
    return (operation == MutableEntryOperation.NONE && cachedValue != null && !cachedValue.isExpiredAt(now)) || value != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove() {
    operation = (operation == MutableEntryOperation.CREATE) ? MutableEntryOperation.NONE : MutableEntryOperation.REMOVE;
    value = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setValue(V value) {
    if (value == null) {
      throw new NullPointerException();
    }
    operation = cachedValue == null || cachedValue.isExpiredAt(now) ? MutableEntryOperation.CREATE : MutableEntryOperation.UPDATE;
    this.value = value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T unwrap(Class<T> clazz) {
    throw new IllegalArgumentException("Can't unwrap an EntryProcessor Entry");
  }

  /**
   * Return the operation
   */
  public MutableEntryOperation getOperation() {
    return operation;
  }

}
