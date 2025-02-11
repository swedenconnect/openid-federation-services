/*
 * Copyright 2024-2025 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package se.digg.oidfed.common.entity.integration;

import java.io.Serializable;

/**
 * Generic cache interface
 *
 * @param <K> type of key
 * @param <V> type of value
 *
 * @author Felix Hellman
 */
public interface Cache<K extends Serializable, V> {
  /**
   * @param key   for this value
   * @param value to add
   */
  void add(K key, Expirable<V> value);

  /**
   * @param key for value to fetch
   * @return value or null if empty
   */
  V get(K key);

  /**
   * @param key to check if it has expired or not
   * @return true if expired or null otherwise false
   */
  boolean shouldRefresh(K key);
}
